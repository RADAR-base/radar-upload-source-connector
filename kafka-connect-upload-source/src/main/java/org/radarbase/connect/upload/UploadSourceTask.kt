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

package org.radarbase.connect.upload

import org.apache.kafka.connect.errors.ConnectException
import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.source.SourceTask
import org.radarbase.connect.upload.UploadSourceConnectorConfig.Companion.SOURCE_POLL_INTERVAL_CONFIG
import org.radarbase.connect.upload.api.*
import org.radarbase.connect.upload.converter.Converter
import org.radarbase.connect.upload.converter.Converter.Companion.END_OF_RECORD_KEY
import org.radarbase.connect.upload.converter.Converter.Companion.RECORD_ID_KEY
import org.radarbase.connect.upload.converter.Converter.Companion.REVISION_KEY
import org.radarbase.connect.upload.converter.ConverterLogRepository
import org.radarbase.connect.upload.converter.LogRepository
import org.radarbase.connect.upload.exception.ConflictException
import org.radarbase.connect.upload.exception.ConversionFailedException
import org.radarbase.connect.upload.util.VersionUtil
import org.slf4j.LoggerFactory

class UploadSourceTask : SourceTask() {
    private var pollInterval: Long = 60_000L
    private lateinit var uploadClient: UploadBackendClient
    private lateinit var converters: List<Converter>
    private lateinit var logRepository: LogRepository

    override fun start(props: Map<String, String>?) {
        val connectConfig = UploadSourceConnectorConfig(props!!)
        val httpClient = connectConfig.httpClient

        uploadClient = UploadBackendClient(
                connectConfig.getAuthenticator(),
                httpClient,
                connectConfig.uploadBackendBaseUrl)

        // init converters if configured
        converters = connectConfig.converterClasses.map {
            try {
                val converterClass = Class.forName(it)
                converterClass.getDeclaredConstructor().newInstance() as Converter
            } catch (exe: Exception) {
                when (exe) {
                    is ClassNotFoundException -> throw ConnectException("Converter class $it not found in class path", exe)
                    is IllegalAccessException, is InstantiationException -> throw ConnectException("Converter class $it could not be instantiated", exe)
                    else -> throw ConnectException("Cannot successfully initialize converter $it", exe)
                }
            }
        }

        pollInterval = connectConfig.getLong(SOURCE_POLL_INTERVAL_CONFIG)

        logRepository = ConverterLogRepository(uploadClient)

        for (converter in converters) {
            val config = uploadClient.requestConnectorConfig(converter.sourceType)
            converter.initialize(config, uploadClient, logRepository, props)
        }


        logger.info("Poll with interval $pollInterval milliseconds")
        logger.info("Initialized ${converters.size} converters...")
    }

    override fun stop() {
        logger.debug("Stopping source task")
        uploadClient.close()
        converters.forEach(Converter::close)
    }

    override fun version(): String = VersionUtil.getVersion()

    override fun poll(): List<SourceRecord> {
        logger.info("Polling new records...")
        while (true) {
            var records: List<RecordDTO>? = null
            try {
                records = uploadClient.pollRecords(PollDTO(1, converters.map { it.sourceType })).records
                logger.info("Received ${records.size} records")
            } catch (exe: Exception) {
                logger.info("Could not successfully poll records. Waiting for next polling...")
            }
            if(records != null) {
                records@ for (record in records) {
                    val converter = converters.find { it.sourceType == record.sourceType }
                    try {
                        if (converter == null) {
                            uploadClient.updateStatus(
                                    record.id!!,
                                    record.metadata!!.copy(
                                            status = "FAILED",
                                            message = "No registered converter found for ${record.sourceType}.")
                            )
                            logger.error("Could not find converter ${record.sourceType} for record ${record.id}")
                            continue@records
                        } else {
                            record.metadata = uploadClient.updateStatus(
                                    record.id!!,
                                    record.metadata!!.copy(status = "PROCESSING")
                            )
                            logger.debug("Updated metadata ${record.id} to PROCESSING")
                        }
                    } catch (exe: Exception) {
                        when(exe) {
                            is ConflictException -> {
                                logger.warn("Conflicting request was made. Skipping this record")
                                continue@records
                            }
                            else -> throw exe
                        }
                    }

                    try {
                        val result = converter.convert(record)
                        result.result?.takeIf(List<*>::isNotEmpty)?.let {
                            return@poll it
                        }
                    } catch (exe: ConversionFailedException) {
                        logger.error("Could not convert record ${record.id}", exe)
                        updateRecordFailure(record)
                    }

                }
            }

            Thread.sleep(pollInterval)
        }
    }

    private fun updateRecordFailure(record: RecordDTO, reason: String? = "Could not convert this record. Please refer to the conversion logs for more details") {
        logger.info("Update record conversion failure")
        val metadata = uploadClient.retrieveRecordMetadata(record.id!!)
        uploadClient.updateStatus(record.id!!, metadata.copy(
            status = "FAILED",
            message = reason
        ))
    }

    override fun commitRecord(record: SourceRecord?) {
        record ?: return

        val offset = record.sourceOffset()

        val recordId = offset[RECORD_ID_KEY] as? Number ?: return
        val revision = offset[REVISION_KEY] as? Number ?: return
        val endOfRecord = offset[END_OF_RECORD_KEY] as? Boolean ?: return

        if (endOfRecord) {
            logger.info("Committing last record of Record $recordId, with Revision $revision")
            val updatedMetadata = uploadClient.updateStatus(recordId.toLong(), RecordMetadataDTO(revision = revision.toInt(), status = "SUCCEEDED", message = "Record has been processed successfully"))

            if(updatedMetadata.status == "SUCCEEDED") {
                logger.info("Uploading logs to backend")
                logRepository.uploadLogs(recordId.toLong())
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UploadSourceTask::class.java)
    }

}
