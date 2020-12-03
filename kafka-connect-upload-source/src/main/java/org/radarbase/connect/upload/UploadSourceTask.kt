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
import java.time.temporal.Temporal
import java.time.temporal.TemporalAmount
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit

class UploadSourceTask : SourceTask() {
    private var queueSize: Int = 1000
    private var pollInterval = Duration.ofMinutes(1)
    private var failedPollInterval = Duration.ofSeconds(6)
    private lateinit var uploadClient: UploadBackendClient
    private lateinit var converters: Map<String, Converter>
    private lateinit var logRepository: LogRepository
    private lateinit var queue: BlockingQueue<SourceRecord>
    private lateinit var converterManager: ConverterManager

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

        // TODO: make queue size a variable
        queueSize = 1000
        queue = ArrayBlockingQueue(queueSize)
        converterManager = ConverterManager(queue, converters, uploadClient, logRepository, pollInterval)

        logger.info("Poll with interval $pollInterval milliseconds")
        logger.info("Initialized ${converters.size} converters...")
    }

    override fun stop() {
        logger.debug("Stopping source task")
        converterManager.close()
        uploadClient.close()
        converters.values.forEach(Converter::close)
    }

    override fun version(): String = VersionUtil.getVersion()

    override fun poll(): List<SourceRecord>? {
        val records = generateSequence { queue.poll() }  // this will retrieve all non-blocking elements
            .take(queueSize) // don't process more than queueSize records at once
            .toList()
        return if (records.isNotEmpty()) {
            records
        } else {
            // if no non-blocking elements are available, it's ok to wait for them for a bit.
            queue.poll(pollInterval.toMillis(), TimeUnit.MILLISECONDS)
                ?.let { listOf(it) }
        }

//        val timeout = nextPoll.untilNow().toMillis()
//        if (timeout > 0) {
//            logger.info("Waiting {} milliseconds for next polling time", timeout)
//            Thread.sleep(timeout)
//        }

        // TODO: retrieve data and convert it in ConverterManager instead
//
//        logger.info("Polling new records...")
//        val records: List<RecordDTO> = try {
//            uploadClient.pollRecords(PollDTO(1, converters.keys)).records
//        } catch (exe: Throwable) {
//            logger.error("Could not successfully poll records. Waiting for next polling...")
//            nextPoll = failedPollInterval.fromNow()
//            return null
//        }
//
//        nextPoll = pollInterval.fromNow()
//
//        logger.info("Received ${records.size} records at $nextPoll")
//
//        return records
//                .flatMap { record -> processRecord(record) ?: emptyList() }
//                .takeIf { it.isNotEmpty() }
    }

    override fun commitRecord(record: SourceRecord?) {
        record ?: return

        val offset = record.sourceOffset()

        val recordId = offset[RECORD_ID_KEY] as? Number ?: return
        val revision = offset[REVISION_KEY] as? Number ?: return
        val endOfRecord = offset[END_OF_RECORD_KEY] as? Boolean ?: return

        if (endOfRecord) {
            logger.info("Committing last record of Record $recordId, with Revision $revision")
            val updatedMetadata = uploadClient.updateStatus(
                    recordId.toLong(),
                    RecordMetadataDTO(revision = revision.toInt(), status = "SUCCEEDED", message = "Record has been processed successfully"))

            if (updatedMetadata.status == "SUCCEEDED") {
                logger.info("Uploading logs to backend")
                converterManager.uploadLogs(recordId.toLong())
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UploadSourceTask::class.java)

        internal fun Temporal.untilNow(): Duration = Duration.between(Instant.now(), this)
        private fun TemporalAmount.fromNow(): Instant = Instant.now().plus(this)
    }
}
