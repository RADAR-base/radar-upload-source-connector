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

package org.radarbase.connect.upload.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Queue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class ConverterLogRepository : LogRepository {
    private val logContainer = ConcurrentHashMap<Long, ConcurrentLinkedQueue<LogRecord>>()

    override val recordIds: Set<Long>
        get() = logContainer.keys

    override fun createLogger(logger: Logger, recordId: Long): RecordLogger = QueueRecordLogger(logger, recordId, get(recordId))

    override fun createLogger(clazz: Class<*>, recordId: Long): RecordLogger = createLogger(LoggerFactory.getLogger(clazz), recordId)

    private fun get(recordId: Long): Queue<LogRecord> =
        logContainer.computeIfAbsent(recordId) { ConcurrentLinkedQueue() }

    override fun extract(recordId: Long, reset: Boolean): Log? {
        val recordQueue = if (reset) {
            logContainer.remove(recordId)
        } else {
            logContainer[recordId]
        }
        return recordQueue
                ?.takeIf { it.isNotEmpty() }
                ?.let { Log(recordId, it) }
    }
}
