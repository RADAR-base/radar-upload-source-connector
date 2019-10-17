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

import okio.BufferedSink
import org.slf4j.Logger
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

enum class LogLevel {
    INFO, DEBUG, WARN, ERROR
}

data class LogRecord(
        val logLevel: LogLevel,
        val message: String,
        val time: Instant = Instant.now())

data class Log(val recordId: Long, val records: Collection<LogRecord>) {
    fun asString(writer: BufferedSink) {
        records.forEach { log ->
            writer.writeUtf8("${log.time} - [${log.logLevel}] ${log.message}\n")
        }
    }
}

interface LogRepository {
    fun info(logger: Logger, recordId: Long, logMessage: String)
    fun debug(logger: Logger, recordId: Long, logMessage: String)
    fun warn(logger: Logger, recordId: Long, logMessage: String)
    fun error(logger: Logger, recordId: Long, logMessage: String, exe: Exception? = null)
    val recordIds: Set<Long>
    fun extract(recordId: Long, reset: Boolean = false): Log?
}

class ConverterLogRepository : LogRepository {
    private val logContainer = ConcurrentHashMap<Long, ConcurrentLinkedQueue<LogRecord>>()

    private fun get(recordId: Long): ConcurrentLinkedQueue<LogRecord> =
            logContainer.getOrPut(recordId, { ConcurrentLinkedQueue() })

    override fun info(logger: Logger, recordId: Long, logMessage: String) {
        get(recordId).add(LogRecord(LogLevel.INFO, logMessage))
        logger.info(logMessage)
    }

    override fun debug(logger: Logger, recordId: Long, logMessage: String) {
        get(recordId).add(LogRecord(LogLevel.DEBUG, logMessage))
        logger.debug(logMessage)
    }

    override fun warn(logger: Logger, recordId: Long, logMessage: String) {
        get(recordId).add(LogRecord(LogLevel.WARN, logMessage))
        logger.warn(logMessage)
    }

    override fun error(logger: Logger, recordId: Long, logMessage: String, exe: Exception?) {
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

        get(recordId).add(LogRecord(LogLevel.ERROR, message))
        logger.error(logMessage, exe)
    }

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
}
