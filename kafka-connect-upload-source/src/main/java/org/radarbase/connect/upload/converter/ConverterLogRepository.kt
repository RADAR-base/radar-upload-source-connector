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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class ConverterLogRepository : LogRepository {
    override fun createLogger(logger: Logger, recordId: Long): RecordLogger = RecordLoggerImpl(logger, recordId, get(recordId))

    override fun createLogger(clazz: Class<*>, recordId: Long): RecordLogger = createLogger(LoggerFactory.getLogger(clazz), recordId)

    private val logContainer = ConcurrentHashMap<Long, ConcurrentLinkedQueue<LogRecord>>()

    private fun get(recordId: Long): Queue<LogRecord> =
            logContainer.getOrPut(recordId, { ConcurrentLinkedQueue() })

    override val recordIds: Set<Long>
        get() = logContainer.keys

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

    private class RecordLoggerImpl(
            private val logger: Logger,
            private val recordId: Long,
            private val container: Queue<LogRecord>
    ) : RecordLogger {
        override fun info(logMessage: String) {
            container.add(LogRecord(LogLevel.INFO, logMessage))
            logger.info("[record {}] {}", recordId, logMessage)
        }

        override fun debug(logMessage: String) {
            container.add(LogRecord(LogLevel.DEBUG, logMessage))
            logger.debug("[record {}] {}", recordId, logMessage)
        }

        override fun warn(logMessage: String) {
            container.add(LogRecord(LogLevel.WARN, logMessage))
            logger.warn("[record {}] {}", recordId, logMessage)
        }

        override fun error(logMessage: String, exe: Exception?) {
            val message = if (exe != null) {
                val trace = ByteArrayOutputStream().use { byteOut ->
                    PrintStream(byteOut).use { printOut ->
                        exe.printStackTrace(printOut)
                    }
                    byteOut.toString("UTF-8")
                }
                "$logMessage: $exe$trace"
            } else {
                logMessage
            }

            container.add(LogRecord(LogLevel.ERROR, message))
            logger.error("[record {}] {}", recordId, logMessage, exe)
        }
    }
}
