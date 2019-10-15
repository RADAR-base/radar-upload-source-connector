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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class ConverterLogRepository : LogRepository {
    override fun uploadLogger(logger: Logger): UploadLogger = UploadLoggerImpl(logger)

    override fun uploadLogger(clazz: Class<*>): UploadLogger = uploadLogger(LoggerFactory.getLogger(clazz))

    private val logContainer = ConcurrentHashMap<Long, ConcurrentLinkedQueue<LogRecord>>()

    private fun get(recordId: Long): ConcurrentLinkedQueue<LogRecord> =
            logContainer.getOrPut(recordId, { ConcurrentLinkedQueue() })

    override fun info(logger: Logger, recordId: Long, logMessage: String) {
        get(recordId).add(LogRecord(LogLevel.INFO, logMessage))
        logger.info("[record {}] {}", recordId, logMessage)
    }

    override fun debug(logger: Logger, recordId: Long, logMessage: String) {
        get(recordId).add(LogRecord(LogLevel.DEBUG, logMessage))
        logger.debug("[record {}] {}", recordId, logMessage)
    }

    override fun warn(logger: Logger, recordId: Long, logMessage: String) {
        get(recordId).add(LogRecord(LogLevel.WARN, logMessage))
        logger.warn("[record {}] {}", recordId, logMessage)
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
        logger.error("[record {}] {}", recordId, logMessage, exe)
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

    private inner class UploadLoggerImpl(private val logger: Logger) : UploadLogger {
        override fun recordLogger(recordId: Long): RecordLogger = RecordLoggerImpl(recordId)

        override fun info(recordId: Long, logMessage: String) = info(logger, recordId, logMessage)

        override fun debug(recordId: Long, logMessage: String) = debug(logger, recordId, logMessage)

        override fun warn(recordId: Long, logMessage: String) = warn(logger, recordId, logMessage)

        override fun error(recordId: Long, logMessage: String, exe: Exception?) = error(logger, recordId, logMessage, exe)

        private inner class RecordLoggerImpl(private val recordId: Long) : RecordLogger {
            override fun info(logMessage: String) = info(logger, recordId, logMessage)

            override fun debug(logMessage: String) = debug(logger, recordId, logMessage)

            override fun warn(logMessage: String) = warn(logger, recordId, logMessage)

            override fun error(logMessage: String, exe: Exception?) = error(logger, recordId, logMessage, exe)
        }
    }
}
