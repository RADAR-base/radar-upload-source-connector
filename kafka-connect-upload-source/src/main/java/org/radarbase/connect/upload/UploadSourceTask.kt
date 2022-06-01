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

import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.source.SourceTask
import org.radarbase.connect.upload.UploadSourceConnectorConfig.Companion.SOURCE_POLL_INTERVAL_CONFIG
import org.radarbase.connect.upload.UploadSourceConnectorConfig.Companion.SOURCE_QUEUE_SIZE_CONFIG
import org.radarbase.connect.upload.api.RecordMetadataDTO
import org.radarbase.connect.upload.api.UploadBackendClient
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.ConverterFactory.Converter
import org.radarbase.connect.upload.converter.ConverterFactory.Converter.Companion.END_OF_RECORD_KEY
import org.radarbase.connect.upload.converter.ConverterFactory.Converter.Companion.RECORD_ID_KEY
import org.radarbase.connect.upload.converter.ConverterFactory.Converter.Companion.REVISION_KEY
import org.radarbase.connect.upload.logging.ConverterLogRepository
import org.radarbase.connect.upload.logging.LogRepository
import org.radarbase.connect.upload.util.VersionUtil
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.temporal.Temporal
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class UploadSourceTask : SourceTask() {
    private var queueSize: Int = 1000
    private var pollInterval = Duration.ofMinutes(1)
    private lateinit var uploadClient: UploadBackendClient
    private lateinit var converters: Map<String, Converter>
    private lateinit var logRepository: LogRepository
    private lateinit var queue: BlockingQueue<SourceRecord>
    private lateinit var converterManager: ConverterManager
    private val commitCounter = AtomicLong(0)
    private lateinit var commitTimer: Timer

    private lateinit var nextPoll: Instant

    override fun start(props: Map<String, String>?) {
        val connectConfig = UploadSourceConnectorConfig(requireNotNull(props))
        nextPoll = Instant.EPOCH
        uploadClient = UploadBackendClient(
            connectConfig.authenticator,
            connectConfig.httpClient,
            connectConfig.uploadBackendBaseUrl,
        )

        logRepository = ConverterLogRepository()

        // init converters if configured
        converters = connectConfig.converterClasses
                .map { className -> ConverterFactory.createConverter(className, props, uploadClient, logRepository)
                        .let { it.sourceType to it }}
                .toMap()

        val pollIntervalMs = connectConfig.getLong(SOURCE_POLL_INTERVAL_CONFIG)
        pollInterval = Duration.ofMillis(pollIntervalMs)

        queueSize = connectConfig.getInt(SOURCE_QUEUE_SIZE_CONFIG)
        queue = ArrayBlockingQueue(queueSize)
        converterManager = ConverterManager(queue, converters, uploadClient, logRepository, pollInterval)

        commitTimer = Timer(true)
        commitTimer.schedule(object : TimerTask() {
            override fun run() {
                logger.info(
                    "Committed {} records in the last {} seconds",
                    commitCounter.getAndSet(0),
                    pollIntervalMs / 1000,
                )
            }
        }, pollIntervalMs, pollIntervalMs)

        logger.info("Initialized ${converters.size} converters...")
    }

    override fun stop() {
        logger.debug("Stopping source task")
        converterManager.close()
        uploadClient.close()
        commitTimer.cancel()
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
    }

    override fun commitRecord(record: SourceRecord?, recordMetadata: RecordMetadata?) {
        record ?: return

        commitCounter.incrementAndGet()

        val offset = record.sourceOffset()

        val recordId = offset[RECORD_ID_KEY] as? Number ?: return
        val revision = offset[REVISION_KEY] as? Number ?: return
        val endOfRecord = offset[END_OF_RECORD_KEY] as? Boolean ?: return

        if (endOfRecord) {
            val updatedMetadata = uploadClient.updateStatus(
                recordId.toLong(),
                RecordMetadataDTO(
                    revision = revision.toInt(),
                    status = "SUCCEEDED",
                    message = "Record has been processed successfully",
                ),
            )

            if (updatedMetadata.status == "SUCCEEDED") {
                logger.info("Uploading logs to backend")
                converterManager.uploadLogs(recordId.toLong())
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UploadSourceTask::class.java)

        internal fun Temporal.untilNow(): Duration = Duration.between(Instant.now(), this)
    }
}
