package org.radarbase.connect.upload

import org.apache.kafka.connect.source.SourceRecord
import org.radarbase.connect.upload.api.PollDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.api.UploadBackendClient
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.LogRepository
import org.radarbase.connect.upload.converter.RecordLogger
import org.radarbase.connect.upload.exception.ConflictException
import org.radarbase.connect.upload.exception.ConversionFailedException
import org.radarbase.connect.upload.exception.ConversionTemporarilyFailedException
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.time.Duration
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ConverterManager(
    private val queue: BlockingQueue<SourceRecord>,
    private val converters: Map<String, ConverterFactory.Converter>,
    private val uploadClient: UploadBackendClient,
    private val logRepository: LogRepository,
    pollDuration: Duration,
): Closeable
{
    private val executor = Executors.newSingleThreadScheduledExecutor()

    init {
        logger.info("Poll with interval {}", pollDuration)
        executor.scheduleAtFixedRate(
            ::poll,
            pollDuration.toMillis() / 2,
            pollDuration.toMillis(),
            TimeUnit.MILLISECONDS,
        )
    }

    private fun poll() {
        logger.info("Polling new records...")
        val records = try {
            uploadClient.pollRecords(PollDTO(1, converters.keys)).records
        } catch (exe: Throwable) {
            logger.error("Could not successfully poll records. Waiting for next polling...", exe)
            return
        }
        for (record in records) {
            val recordLogger = logRepository.createLogger(logger, requireNotNull(record.id))
            try {
                processRecord(record, recordLogger) {
                    queue.put(it)
                }
            } catch (ex: Throwable) {
                recordLogger.error("Cannot convert record", ex)
            }
        }
    }

    private fun processRecord(
        record: RecordDTO,
        recordLogger: RecordLogger,
        produce: (SourceRecord) -> Unit,
    ) {
        try {
            val converter = converters[record.sourceType]
                ?: throw ConversionTemporarilyFailedException("Could not find converter ${record.sourceType} for record ${record.id}")

            markProcessing(record, recordLogger) ?: return

            converter.convert(record, produce)
        } catch (exe: ConversionFailedException) {
            recordLogger.error("Could not convert record")
            updateRecordFailure(record, recordLogger, exe)
            throw exe
        } catch (exe: ConversionTemporarilyFailedException) {
            recordLogger.error("Could not convert record due to temporary failure")
            updateRecordTemporaryFailure(record, recordLogger, exe)
            throw exe
        } catch (exe: Throwable) {
            recordLogger.error("Could not convert record due to application failure", exe)
            updateRecordTemporaryFailure(record, recordLogger,
                ConversionTemporarilyFailedException("Could not convert record ${record.id} due to application failure", exe))
            throw exe
        }
    }

    private fun markProcessing(
        record: RecordDTO,
        recordLogger: RecordLogger,
    ): RecordDTO? {
        return try {
            record.apply {
                metadata = uploadClient.updateStatus(
                    record.id!!,
                    record.metadata!!.copy(status = "PROCESSING")
                )
                recordLogger.debug("Updated metadata to PROCESSING")
            }
        } catch (exe: ConflictException) {
            recordLogger.warn("Conflicting request was made. Skipping this record")
            null
        } catch (exe: Exception) {
            throw ConversionTemporarilyFailedException("Cannot update record metadata", exe)
        }
    }

    private fun updateRecordFailure(
        record: RecordDTO,
        recordLogger: RecordLogger,
        exe: Exception,
        reason: String = "Could not convert this record. Please refer to the conversion logs for more details",
    ) {
        val recordId = record.id ?: return
        recordLogger.error("$reason: ${exe.message}", exe.cause)
        val metadata = uploadClient.retrieveRecordMetadata(recordId)
        val updatedMetadata = uploadClient.updateStatus(recordId, metadata.copy(
            status = "FAILED",
            message = reason
        ))

        if (updatedMetadata.status == "FAILED") {
            logger.info("Uploading logs to backend")
            uploadLogs(recordId)
        }
    }

    private fun updateRecordTemporaryFailure(
        record: RecordDTO,
        recordLogger: RecordLogger,
        exe: Exception,
        reason: String = "Temporarily could not convert this record. Please refer to the conversion logs for more details",
    ) {
        logger.info("Update record conversion failure")
        recordLogger.error("$reason: ${exe.message}", exe.cause)
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

    fun uploadAllLogs(reset: Boolean = true) {
        logger.info("Uploading all remaining logs")
        logRepository.recordIds.forEach { r ->
            logRepository.extract(r, reset)
                ?.let { uploadClient.addLogs(it) }
        }
        logger.info("All record logs are uploaded")
    }

    fun uploadLogs(recordId: Long, reset: Boolean = true) {
        logger.info("Sending record $recordId logs...")
        logRepository.extract(recordId, reset)
            ?.let { uploadClient.addLogs(it) }
    }

    override fun close() {
        // this will trigger a interrupt on the put method
        executor.shutdownNow()
        uploadAllLogs()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ConverterManager::class.java)
    }
}
