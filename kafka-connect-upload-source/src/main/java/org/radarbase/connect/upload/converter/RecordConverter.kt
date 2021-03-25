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
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.api.UploadBackendClient
import org.radarbase.connect.upload.converter.ConverterFactory.Converter.Companion.END_OF_RECORD_KEY
import org.radarbase.connect.upload.exception.ConversionFailedException
import org.radarbase.connect.upload.exception.ConversionTemporarilyFailedException
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.nio.file.Paths

class RecordConverter(
    override val sourceType: String,
    processorFactories: List<FileProcessorFactory>,
    private val client: UploadBackendClient,
    private val logRepository: LogRepository,
    private val avroData: AvroData = createAvroData(),
) : ConverterFactory.Converter {
    private val delegatingProcessor = DelegatingProcessor(
        processorFactories = processorFactories,
        tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "upload-connector", "$sourceType-cache"),
        generateTempFilePrefix = { context ->
            "record-${context.id}-"
        },
    )

    override fun convert(
        record: RecordDTO,
        produce: (SourceRecord) -> Unit,
    ) {
        convertStream(
            record,
            openStream = { context, mapStream ->
                client.retrieveFile(context.record, context.fileName) { body ->
                    mapStream(body.byteStream())
                }
            },
            produce,
        )
    }

    override fun convertStream(
        record: RecordDTO,
        openStream: (ConverterFactory.ContentsContext, (InputStream) -> Unit) -> Unit,
        produce: (SourceRecord) -> Unit,
    ) {
        val contexts = ConverterFactory.ContentsContext.createAll(record, logRepository, avroData)

        try {
            // Cannot submit data immediately: we need to mark the last data record
            // Once a new data point is generated, we can submit the previous data record. After
            // all data points are mapped, submit the last data record with a key setting it to
            // end of record.
            var previousData: SourceRecord? = null

            contexts.forEach { context ->
                try {
                    openStream(context) { stream ->
                        convertFile(context, stream) { topicData ->
                            val currentData = previousData
                            previousData = topicData.toSourceRecord(context, getPartition())
                            if (currentData != null) {
                                produce(currentData)
                            }
                        }
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

    override fun convertFile(
        context: ConverterFactory.ContentsContext,
        inputStream: InputStream,
        produce: (TopicData) -> Unit,
    ) {
        try {
            delegatingProcessor.processData(context, inputStream, produce)
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

        fun createAvroData(): AvroData = AvroData(
            AvroDataConfig.Builder()
                .with(AvroDataConfig.CONNECT_META_DATA_CONFIG, false)
                .with(AvroDataConfig.SCHEMAS_CACHE_SIZE_CONFIG, 20)
                .with(AvroDataConfig.ENHANCED_AVRO_SCHEMA_SUPPORT_CONFIG, true)
                .build()
        )
    }
}
