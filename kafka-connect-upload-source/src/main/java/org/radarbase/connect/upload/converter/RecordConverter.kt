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
import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.api.RecordDataDTO
import org.radarbase.connect.upload.api.UploadBackendClient
import org.radarbase.connect.upload.converter.ConverterFactory.Converter.Companion.END_OF_RECORD_KEY
import org.radarbase.connect.upload.converter.ConverterFactory.Converter.Companion.RECORD_ID_KEY
import org.radarbase.connect.upload.converter.ConverterFactory.Converter.Companion.REVISION_KEY
import org.radarbase.connect.upload.exception.ConversionFailedException
import org.radarbase.connect.upload.exception.ConversionTemporarilyFailedException
import org.radarcns.kafka.ObservationKey
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream

class RecordConverter(
        override val sourceType: String,
        private val processorFactories: List<FileProcessorFactory>,
        private val client: UploadBackendClient,
        logRepository: LogRepository,
        private val avroData: AvroData = AvroData(20)
) : ConverterFactory.Converter {
    private val uploadLogger: UploadLogger = logRepository.uploadLogger(logger)

    override fun convert(record: RecordDTO): List<SourceRecord> {
        val recordId = checkNotNull(record.id)
        uploadLogger.info(recordId,"Converting record: record-id $recordId")

        try {
            val recordData = checkNotNull(record.data) { "Record data cannot be null" }
            val recordFileNames = checkNotNull(recordData.contents) { "Record data has empty content" }
            val recordMetadata = checkNotNull(record.metadata) { "Record meta-data cannot be null" }

            val key = recordData.computeObservationKey(avroData)

            return recordFileNames
                    .flatMap { contents ->
                        client.retrieveFile(record, contents.fileName) { body ->
                            convertFile(record, contents, body.byteStream())
                        }
                    }
                    .map { topicData ->
                        val valRecord = avroData.toConnectData(topicData.value.schema, topicData.value)
                        val offset = mutableMapOf(
                                END_OF_RECORD_KEY to topicData.endOfFileOffSet,
                                RECORD_ID_KEY to recordId,
                                REVISION_KEY to recordMetadata.revision
                        )
                        SourceRecord(getPartition(), offset, topicData.topic, key.schema(), key.value(), valRecord.schema(), valRecord.value())
                    }
        } catch (exe: IOException) {
            uploadLogger.error(recordId, "Temporarily could not convert record $recordId", exe)
            throw ConversionTemporarilyFailedException("Temporarily could not convert record $recordId", exe)
        }
    }

    override fun convertFile(record: RecordDTO, contents: ContentsDTO, inputStream: InputStream): List<FileProcessorFactory.TopicData> {
        val processorFactory = processorFactories.firstOrNull { it.matches(contents) }
                ?: throw ConversionFailedException("Cannot find data processor for record ${record.id} with file ${contents.fileName}")

        try {
            return processorFactory
                    .fileProcessor(record)
                    .processData(contents, inputStream, System.currentTimeMillis() / 1000.0)
                    .also { it.lastOrNull()?.endOfFileOffSet = true }
        } catch (exe: Exception) {
            uploadLogger.error(record.id!!, "Could not convert record ${record.id}", exe)
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
    }
}
