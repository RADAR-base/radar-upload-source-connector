/*
 *
 *  * Copyright 2019 The Hyve
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */

package org.radarbase.connect.upload.converter

import io.confluent.connect.avro.AvroData
import org.apache.kafka.connect.data.SchemaAndValue
import org.apache.kafka.connect.source.SourceRecord
import org.radarbase.connect.upload.converter.ConverterFactory.Converter.Companion.END_OF_RECORD_KEY
import org.radarbase.connect.upload.converter.ConverterFactory.Converter.Companion.RECORD_ID_KEY
import org.radarbase.connect.upload.converter.ConverterFactory.Converter.Companion.REVISION_KEY
import org.radarbase.connect.upload.exception.ConversionFailedException
import org.radarbase.connect.upload.exception.ConversionTemporarilyFailedException
import org.radarcns.kafka.ObservationKey
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import io.confluent.connect.avro.AvroDataConfig
import org.radarbase.connect.upload.api.*
import org.radarbase.connect.upload.exception.DataProcessorNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class RecordConverter(
    override val sourceType: String,
    private val processorFactories: List<FileProcessorFactory>,
    private val client: UploadBackendClient,
    private val logRepository: LogRepository,
    private val avroData: AvroData = AvroData(AvroDataConfig.Builder()
            .with(AvroDataConfig.CONNECT_META_DATA_CONFIG, false)
            .with(AvroDataConfig.SCHEMAS_CACHE_SIZE_CONFIG, 20)
            .with(AvroDataConfig.ENHANCED_AVRO_SCHEMA_SUPPORT_CONFIG, true)
            .build())
) : ConverterFactory.Converter {

    private val tempDir: Path = Paths.get(System.getProperty("java.io.tmpdir"), "upload-connector", "$sourceType-cache")

    init {
        if (Files.exists(tempDir)) {
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach(Files::delete)
        }
        Files.createDirectories(tempDir)
    }

    override fun convert(
        record: RecordDTO,
        produce: (SourceRecord) -> Unit,
    ) {
        convertStream(
            record,
            useStream = { r, contents, mapStream ->
                client.retrieveFile(r, contents.fileName) { body ->
                    mapStream(body.byteStream())
                }
            },
            produce,
        )
    }


    override fun convertStream(
        record: RecordDTO,
        useStream: (RecordDTO, ContentsDTO, (InputStream) -> Unit) -> Unit,
        produce: (SourceRecord) -> Unit,
    ) {
        val recordId = checkNotNull(record.id)
        val recordLogger = logRepository.createLogger(logger, recordId)
        recordLogger.info("Converting record: record-id $recordId")

        try {
            val recordData = checkNotNull(record.data) { "Record data cannot be null" }
            val recordFileNames = checkNotNull(recordData.contents) { "Record data has empty content" }
            val recordMetadata = checkNotNull(record.metadata) { "Record meta-data cannot be null" }

            val key = recordData.computeObservationKey(avroData)

            var previousData: TopicData? = null

            recordFileNames.forEach { contents ->
                val produceTopicData: (TopicData) -> Unit = { topicData ->
                    val currentData = previousData
                    previousData = topicData
                    if (currentData != null) {
                        produce(currentData.toSourceRecord(
                            key,
                            recordId,
                            recordMetadata,
                            recordLogger,
                            endOfRecord = false,
                        ))
                    }
                }

                useStream(record, contents) { stream ->
                    convertFile(
                        record,
                        contents,
                        stream,
                        recordLogger,
                        produceTopicData,
                    )
                }
            }
            val lastData = previousData
            if (lastData != null) {
                produce(lastData.toSourceRecord(
                    key,
                    recordId,
                    recordMetadata,
                    recordLogger,
                    endOfRecord = true,
                ))
            }
        } catch (exe: IOException) {
            recordLogger.error("Temporarily could not convert record $recordId", exe)
            throw ConversionTemporarilyFailedException("Temporarily could not convert record $recordId", exe)
        }
    }

    private fun convertDirectly(
        processor: FileProcessorFactory.FileProcessor,
        inputStream: InputStream,
        contents: ContentsDTO,
        timeReceived: Double,
        produce: (TopicData) -> Unit
    ) {
        processor.processData(
            contents,
            inputStream,
            timeReceived,
            produce,
        )
    }

    private fun convertViaTempFile(
        processors: List<FileProcessorFactory.FileProcessor>,
        record: RecordDTO,
        contents: ContentsDTO,
        inputStream: InputStream,
        timeReceived: Double,
        produce: (TopicData) -> Unit
    ) {
        val tempFile = Files.createTempFile(tempDir, "record-${record.id}-", ".bin")
        try {
            inputStream.copyTo(Files.newOutputStream(tempFile))
            processors.forEach { processor ->
                processor.processData(
                    contents,
                    Files.newInputStream(tempFile).buffered(),
                    timeReceived,
                    produce,
                )
            }
        } finally {
            Files.delete(tempFile)
        }
    }

    private fun TopicData.toSourceRecord(
        key: SchemaAndValue,
        recordId: Long,
        recordMetadata: RecordMetadataDTO,
        recordLogger: RecordLogger,
        endOfRecord: Boolean = false,
    ): SourceRecord {
        try {
            val valRecord = avroData.toConnectData(value.schema, value)
            val offset = mutableMapOf(
                END_OF_RECORD_KEY to endOfRecord,
                RECORD_ID_KEY to recordId,
                REVISION_KEY to recordMetadata.revision
            )
            return SourceRecord(
                getPartition(),
                offset,
                topic,
                key.schema(),
                key.value(),
                valRecord.schema(),
                valRecord.value()
            )
        } catch (exe: Exception) {
            recordLogger.info("This value $value and schema ${value.schema.toString(true)} could not be converted")
            throw exe
        }
    }

    override fun convertFile(
        record: RecordDTO,
        contents: ContentsDTO,
        inputStream: InputStream,
        recordLogger: RecordLogger,
        produce: (TopicData) -> Unit,
    ) {
        try {
            val processors = processorFactories.createProcessors(record, contents)
            val timeReceived = System.currentTimeMillis() / 1000.0
            val stream = inputStream.preProcess(processors, contents)
            if (processors.size == 1) {
                convertDirectly(
                    processors.first(),
                    stream,
                    contents,
                    timeReceived,
                    produce,
                )
            } else {
                convertViaTempFile(
                    processors,
                    record,
                    contents,
                    stream,
                    timeReceived,
                    produce,
                )
            }
        } catch (exe: Exception) {
            recordLogger.error("Could not convert record ${record.id}", exe)
            throw ConversionFailedException("Could not convert record ${record.id}",exe)
        }
    }

    override fun getPartition(): MutableMap<String, Any> = mutableMapOf("source-type" to sourceType)

    override fun close() {
        this.client.close()
    }

    private fun RecordDataDTO.computeObservationKey(avroData: AvroData): SchemaAndValue {
        return avroData.toConnectData(
                ObservationKey.getClassSchema(),
                ObservationKey(
                        projectId,
                        userId,
                        sourceId
                )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RecordConverter::class.java)

        fun InputStream.preProcess(
            processors: List<FileProcessorFactory.FileProcessor>,
            contents: ContentsDTO,
        ): InputStream = processors.fold(this) { stream, processor -> processor.preProcessFile(contents, stream) }

        fun Iterable<FileProcessorFactory>.createProcessors(
            record: RecordDTO,
            contents: ContentsDTO,
        ): List<FileProcessorFactory.FileProcessor> {
            val processors = mapNotNull { factory ->
                if (factory.matches(contents)) {
                    logger.info("record {} with contents {} does match {}", record.id, contents, factory.javaClass)
                    factory.createProcessor(record)
                } else {
                    logger.info("record {} with contents {} does not match {}", record.id, contents, factory.javaClass)
                    null
                }
            }
            if (processors.isEmpty()) {
                throw DataProcessorNotFoundException("Cannot find data processor for record ${record.id} with file ${contents.fileName}")
            }
            return processors
        }
    }
}
