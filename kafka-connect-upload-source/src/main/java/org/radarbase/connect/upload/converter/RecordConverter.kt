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
import org.radarbase.connect.upload.UploadSourceConnectorConfig
import org.radarbase.connect.upload.api.*
import org.radarbase.connect.upload.converter.Converter.Companion.END_OF_RECORD_KEY
import org.radarbase.connect.upload.converter.Converter.Companion.RECORD_ID_KEY
import org.radarbase.connect.upload.converter.Converter.Companion.REVISION_KEY
import org.radarbase.connect.upload.api.LogLevel.*
import org.radarbase.connect.upload.exception.ConversionFailedException
import org.radarcns.kafka.ObservationKey
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.lang.Exception
import java.time.Instant


abstract class RecordConverter(override val sourceType: String, val avroData: AvroData = AvroData(20)) : Converter {

    private lateinit var connectorConfig: SourceTypeDTO
    private lateinit var client: UploadBackendClient
    private lateinit var settings: Map<String, String>
    private lateinit var topic: String
    private lateinit var logsRepository: LogRepository
    override fun initialize(
            connectorConfig: SourceTypeDTO,
            client: UploadBackendClient,
            logRepository: LogRepository,
            settings: Map<String, String>) {
        this.connectorConfig = connectorConfig
        this.client = client
        this.settings = settings

        if (this.connectorConfig.topics?.size == 1) {
            this.topic = this.connectorConfig.topics?.first()!!
        }

        this.logsRepository = logRepository
    }

    override fun close() {
        if (this::logsRepository.isInitialized) {
            this.logsRepository.uploadAllLogs()
        }
        if (this::client.isInitialized) {
            this.client.close()
        }
    }

    val logRepository get() = this.logsRepository

    override fun convert(record: RecordDTO): ConversionResult {
        val recordId = record.id!!
        logsRepository.info(logger, recordId,"Converting record : record-id $recordId")

        try {
            record.validateRecord()

            val key = record.computeObservationKey(avroData)

            val recordContents = record.data!!.contents!!

            val sourceRecords = recordContents.map contentMap@{ content ->

                val response = client.retrieveFile(record, content.fileName)
                // if receiving a content fails, mark that record as READY and stop converting
                if(response == null) {
                    client.updateStatus(recordId, record.metadata!!.copy(
                            status = "READY",
                            message = "Could not retrieve file ${content.fileName} from record with id $recordId"
                    ))

                    logsRepository.error(logger, recordId,"Could not retrieve file ${content.fileName} from record with id $recordId")
                    return@convert ConversionResult(record, emptyList())
                }

                val timeReceived = Instant.now().epochSecond

                response.use responseResource@{ res ->
                    return@contentMap processData(content, res.byteStream(), record, timeReceived.toDouble())
                            .map topicDataMap@{ topicData ->
                                val valRecord = avroData.toConnectData(topicData.value.schema, topicData.value)
                                val offset = mutableMapOf(
                                        END_OF_RECORD_KEY to topicData.endOfFileOffSet,
                                        RECORD_ID_KEY to record.id,
                                        REVISION_KEY to record.metadata?.revision
                                )
                                return@topicDataMap SourceRecord(getPartition(), offset, topicData.topic, key.schema(), key.value(), valRecord.schema(), valRecord.value())
                            }
                }
            }
            return ConversionResult(record, sourceRecords.flatMap { it.toList() })
        } catch (exe: Exception){
            logsRepository.error(logger, recordId, "Could not convert record ${recordId}", exe)
            throw ConversionFailedException("Could not convert record ${recordId}",exe)
        }
    }

    private fun commitLogs(record: RecordDTO, client: UploadBackendClient): RecordMetadataDTO {
        logger.debug("Sending record logs..")
        val logs = LogsDto().apply {
                contents = UploadSourceConnectorConfig.mapper.writeValueAsString(logsRepository)
        }
        logger.info(UploadSourceConnectorConfig.mapper.writeValueAsString(logsRepository))
        return client.addLogs(record.id!!, logs)
    }

    /** process file content with the record data. The implementing method should close response-body. */
    abstract fun processData(contents: ContentsDTO, inputStream: InputStream, record: RecordDTO, timeReceived: Double)
            : List<TopicData>

    override fun getPartition(): MutableMap<String, Any> = mutableMapOf("source-type" to sourceType)

    private fun RecordDTO.validateRecord() {
        this.id ?: throw IllegalStateException("Record id cannot be null")
        this.metadata ?: throw IllegalStateException("Record meta-data cannot be null")
        this.data ?: throw IllegalStateException("Record data cannot be null")
        this.data?.contents ?: throw IllegalStateException("Record data has empty content")
    }

    private fun RecordDTO.computeObservationKey(avroData: AvroData): SchemaAndValue {
        val data = this.data ?: throw IllegalStateException("Cannot process record without data")
        return avroData.toConnectData(
                ObservationKey.getClassSchema(),
                ObservationKey(
                        data.projectId,
                        data.userId,
                        data.sourceId
                )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RecordConverter::class.java)
    }


}
