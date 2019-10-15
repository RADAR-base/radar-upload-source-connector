/*
 *  Copyright 2019 The Hyve
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.radarbase.connect.upload.converter

import okio.BufferedSink
import org.slf4j.Logger
import java.time.Instant

interface LogRepository {
    fun uploadLogger(logger: Logger): UploadLogger
    fun uploadLogger(clazz: Class<*>): UploadLogger
    fun info(logger: Logger, recordId: Long, logMessage: String)
    fun debug(logger: Logger, recordId: Long, logMessage: String)
    fun warn(logger: Logger, recordId: Long, logMessage: String)
    fun error(logger: Logger, recordId: Long, logMessage: String, exe: Exception? = null)
    val recordIds: Set<Long>
    fun extract(recordId: Long, reset: Boolean = false): Log?
}

interface UploadLogger {
    fun recordLogger(recordId: Long): RecordLogger
    fun info(recordId: Long, logMessage: String)
    fun debug(recordId: Long, logMessage: String)
    fun warn(recordId: Long, logMessage: String)
    fun error(recordId: Long, logMessage: String, exe: Exception? = null)
}

interface RecordLogger {
    fun info(logMessage: String)
    fun debug(logMessage: String)
    fun warn(logMessage: String)
    fun error(logMessage: String, exe: Exception? = null)
}

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
