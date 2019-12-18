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

import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.source.SourceTask
import org.radarbase.connect.upload.UploadSourceConnectorConfig.Companion.SOURCE_POLL_INTERVAL_CONFIG
import org.radarbase.connect.upload.api.PollDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.api.RecordMetadataDTO
import org.radarbase.connect.upload.api.UploadBackendClient
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.ConverterFactory.Converter
import org.radarbase.connect.upload.converter.ConverterFactory.Converter.Companion.END_OF_RECORD_KEY
import org.radarbase.connect.upload.converter.ConverterFactory.Converter.Companion.RECORD_ID_KEY
import org.radarbase.connect.upload.converter.ConverterFactory.Converter.Companion.REVISION_KEY
import org.radarbase.connect.upload.converter.ConverterLogRepository
import org.radarbase.connect.upload.converter.LogRepository
import org.radarbase.connect.upload.exception.ConflictException
import org.radarbase.connect.upload.exception.ConversionFailedException
import org.radarbase.connect.upload.exception.ConversionTemporarilyFailedException
import org.radarbase.connect.upload.util.VersionUtil
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import java.time.temporal.TemporalAmount
import java.time.temporal.TemporalUnit

class UploadSourceTask : SourceTask() {
    private var pollInterval = Duration.ofMinutes(1)
    private var failedPollInterval = Duration.ofSeconds(6)
    private lateinit var uploadClient: UploadBackendClient
    private lateinit var converters: Map<String, Converter>
    private lateinit var logRepository: LogRepository

    private lateinit var nextPoll: Instant

    override fun start(props: Map<String, String>?) {
        val connectConfig = UploadSourceConnectorConfig(props!!)
        val httpClient = connectConfig.httpClient
        nextPoll = Instant.EPOCH
        uploadClient = UploadBackendClient(
                connectConfig.getAuthenticator(),
                httpClient,
                connectConfig.uploadBackendBaseUrl)

        logRepository = ConverterLogRepository()

        // init converters if configured
        converters = connectConfig.converterClasses
                .map { className -> ConverterFactory.createConverter(className, props, uploadClient, logRepository)
                        .let { it.sourceType to it }}
                .toMap()

        pollInterval = Duration.ofMillis(connectConfig.getLong(SOURCE_POLL_INTERVAL_CONFIG))
        failedPollInterval = pollInterval.dividedBy(10)

        logger.info("Poll with interval $pollInterval milliseconds")
        logger.info("Initialized ${converters.size} converters...")
    }

    override fun stop() {
        logger.debug("Stopping source task")
        uploadAllLogs()
        uploadClient.close()
        converters.values.forEach(Converter::close)
    }

    override fun version(): String = VersionUtil.getVersion()

    override fun poll(): List<SourceRecord> {
        val timeout = nextPoll.untilNow(ChronoUnit.MILLIS)
        if (timeout > 0) {
            logger.info("Waiting {} milliseconds for next polling time", timeout)
            Thread.sleep(timeout)
        }

        logger.info("Polling new records...")
        val records: List<RecordDTO> = try {
            uploadClient.pollRecords(PollDTO(1, converters.keys)).records
        } catch (exe: Exception) {
            logger.info("Could not successfully poll records. Waiting for next polling...")
            nextPoll = failedPollInterval.fromNow()
            return emptyList()
        }

        nextPoll = pollInterval.fromNow()

        logger.info("Received ${records.size} records at $nextPoll")

        return records.flatMap { record -> processRecord(record) ?: emptyList() }
    }

    private fun processRecord(record: RecordDTO): List<SourceRecord>? {
        return try {
            val converter = converters[record.sourceType]
                    ?: throw ConversionTemporarilyFailedException("Could not find converter ${record.sourceType} for record ${record.id}")

            markProcessing(record) ?: return null

            converter.convert(record)
        } catch (exe: ConversionFailedException) {
            logger.error("Could not convert record ${record.id}", exe)
            updateRecordFailure(record, exe)
            null
        } catch (exe: ConversionTemporarilyFailedException) {
            logger.error("Could not convert record ${record.id} due to temporary failure", exe)
            updateRecordTemporaryFailure(record, exe)
            null
        }
    }

    private fun markProcessing(record: RecordDTO): RecordDTO? {
        return try {
            record.apply {
                metadata = uploadClient.updateStatus(
                        record.id!!,
                        record.metadata!!.copy(status = "PROCESSING")
                )
                logger.debug("Updated metadata $id to PROCESSING")
            }
        } catch (exe: ConflictException) {
            logger.warn("Conflicting request was made. Skipping this record")
            null
        } catch (exe: Exception) {
            throw ConversionTemporarilyFailedException("Cannot update record metadata", exe)
        }
    }

    private fun updateRecordFailure(record: RecordDTO, exe: Exception, reason: String = "Could not convert this record. Please refer to the conversion logs for more details") {
        val recordLogger = logRepository.createLogger(logger, record.id!!)
        recordLogger.error(reason, exe)
        val metadata = uploadClient.retrieveRecordMetadata(record.id!!)
        val updatedMetadata = uploadClient.updateStatus(record.id!!, metadata.copy(
                status = "FAILED",
                message = reason
        ))

        if (updatedMetadata.status == "FAILED") {
            logger.info("Uploading logs to backend")
            uploadLogs(record.id!!)
        }
    }

    private fun updateRecordTemporaryFailure(record: RecordDTO, exe: Exception, reason: String = "Temporarily could not convert this record. Please refer to the conversion logs for more details") {
        logger.info("Update record conversion failure")
        val recordLogger = logRepository.createLogger(logger, record.id!!)
        recordLogger.error(reason, exe)
        val metadata = uploadClient.retrieveRecordMetadata(record.id!!)
        val updatedMetadata = uploadClient.updateStatus(record.id!!, metadata.copy(
                status = "READY",
                message = reason
        ))

        if (updatedMetadata.status == "READY") {
            logger.info("Uploading logs to backend")
            uploadLogs(record.id!!)
        }
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

            if (updatedMetadata.status == "SUCCEEDED") {
                logger.info("Uploading logs to backend")
                uploadLogs(recordId.toLong())
            }
        }
    }

    private fun uploadAllLogs(reset: Boolean = true) {
        logger.info("Uploading all remaining logs")
        logRepository.recordIds.forEach { r ->
            logRepository.extract(r, reset)
                    ?.let { uploadClient.addLogs(it) }
        }
        logger.info("All record logs are uploaded")
    }

    private fun uploadLogs(recordId: Long, reset: Boolean = true) {
        logger.info("Sending record $recordId logs...")
        logRepository.extract(recordId, reset)
                ?.let { uploadClient.addLogs(it) }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UploadSourceTask::class.java)

        private fun Temporal.untilNow(unit: TemporalUnit): Long = unit.between(Instant.now(), this)
        private fun TemporalAmount.fromNow(): Instant = Instant.now().plus(this)
    }
}
