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
import io.confluent.connect.avro.AvroDataConfig
import org.apache.kafka.connect.source.SourceRecord
import org.radarbase.connect.upload.api.*
import org.radarbase.connect.upload.converter.ConverterFactory.Converter.Companion.END_OF_RECORD_KEY
import org.radarbase.connect.upload.converter.ConverterFactory.Converter.Companion.RECORD_ID_KEY
import org.radarbase.connect.upload.converter.ConverterFactory.Converter.Companion.REVISION_KEY
import org.radarbase.connect.upload.exception.ConversionFailedException
import org.radarbase.connect.upload.exception.ConversionTemporarilyFailedException
import org.radarbase.connect.upload.exception.DataProcessorNotFoundException
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class RecordConverter(
    override val sourceType: String,
    private val processorFactories: List<FileProcessorFactory>,
    private val client: UploadBackendClient,
    private val logRepository: LogRepository,
    private val avroData: AvroData = createAvroData(),
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
            useStream = { context, mapStream ->
                client.retrieveFile(context.record, context.fileName) { body ->
                    mapStream(body.byteStream())
                }
            },
            produce,
        )
    }

    override fun convertStream(
        record: RecordDTO,
        useStream: (ConverterFactory.ContentsContext, (InputStream) -> Unit) -> Unit,
        produce: (SourceRecord) -> Unit,
    ) {
        val contexts = ConverterFactory.ContentsContext.createAll(record, logRepository, avroData)

        try {
            var previousData: SourceRecord? = null

            contexts.forEach { context ->
                try {
                    val produceTopicData: (TopicData) -> Unit = { topicData ->
                        val currentData = previousData
                        previousData = topicData.toSourceRecord(context)
                        if (currentData != null) {
                            produce(currentData)
                        }
                    }

                    useStream(context) { stream ->
                        convertFile(
                            context,
                            stream,
                            produceTopicData,
                        )
                    }
                } catch (exe: IOException) {
                    context.logger.error("Temporarily could not convert record", exe)
                    throw ConversionTemporarilyFailedException("Temporarily could not convert record ${context.id}", exe)
                }
            }
            val lastData = previousData
            if (lastData != null) {
                @Suppress("UNCHECKED_CAST")
                (lastData.sourceOffset() as MutableMap<String, Any>)[END_OF_RECORD_KEY] = true
                produce(lastData)
            }
        } catch (exe: IOException) {
            logger.error("Temporarily could not convert record ${record.id}", exe)
            throw ConversionTemporarilyFailedException("Temporarily could not convert record ${record.id}", exe)
        }
    }

    private fun convertViaTempFile(
        processors: List<FileProcessorFactory.FileProcessor>,
        context: ConverterFactory.ContentsContext,
        inputStream: InputStream,
        timeReceived: Double,
        produce: (TopicData) -> Unit
    ) {
        val tempFile = Files.createTempFile(tempDir, "record-${context.id}-", ".bin")
        try {
            inputStream.copyTo(Files.newOutputStream(tempFile))
            processors.forEach { processor ->
                processor.processData(
                    context,
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
        context: ConverterFactory.ContentsContext
    ): SourceRecord {
        try {
            val valRecord = avroData.toConnectData(value.schema, value)
            val offset = mutableMapOf(
                END_OF_RECORD_KEY to false,
                RECORD_ID_KEY to context.id,
                REVISION_KEY to context.metadata.revision
            )
            return SourceRecord(
                getPartition(),
                offset,
                topic,
                context.key.schema(),
                context.key.value(),
                valRecord.schema(),
                valRecord.value()
            )
        } catch (exe: Exception) {
            context.logger.info("This value $value and schema ${value.schema.toString(true)} could not be converted")
            throw exe
        }
    }

    override fun convertFile(
        context: ConverterFactory.ContentsContext,
        inputStream: InputStream,
        produce: (TopicData) -> Unit,
    ) {
        try {
            val processors = processorFactories.createProcessors(context)
            val timeReceived = System.currentTimeMillis() / 1000.0
            val stream = inputStream.preProcess(processors, context)
            if (processors.size == 1) {
                processors.first().processData(
                    context,
                    inputStream,
                    timeReceived,
                    produce,
                )
            } else {
                convertViaTempFile(
                    processors,
                    context,
                    stream,
                    timeReceived,
                    produce,
                )
            }
        } catch (exe: Exception) {
            context.logger.error("Could not convert record ${context.id}", exe)
            throw ConversionFailedException("Could not convert record ${context.id}",exe)
        }
    }

    override fun getPartition(): MutableMap<String, Any> = mutableMapOf(
        "source-type" to sourceType,
    )

    override fun close() {
        this.client.close()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RecordConverter::class.java)

        fun InputStream.preProcess(
            processors: List<FileProcessorFactory.FileProcessor>,
            context: ConverterFactory.ContentsContext,
        ): InputStream = processors.fold(this) { stream, processor -> processor.preProcessFile(context, stream) }

        fun Iterable<FileProcessorFactory>.createProcessors(
            context: ConverterFactory.ContentsContext,
        ): List<FileProcessorFactory.FileProcessor> {
            val processors = mapNotNull { factory ->
                if (factory.matches(context.contents)) {
                    factory.createProcessor(context.record)
                } else {
                    null
                }
            }
            if (processors.isEmpty()) {
                throw DataProcessorNotFoundException("Cannot find data processor for record ${context.id} with file ${context.fileName}")
            }
            return processors
        }

        fun createAvroData(): AvroData = AvroData(
            AvroDataConfig.Builder()
                .with(AvroDataConfig.CONNECT_META_DATA_CONFIG, false)
                .with(AvroDataConfig.SCHEMAS_CACHE_SIZE_CONFIG, 20)
                .with(AvroDataConfig.ENHANCED_AVRO_SCHEMA_SUPPORT_CONFIG, true)
                .build()
        )
    }
}
