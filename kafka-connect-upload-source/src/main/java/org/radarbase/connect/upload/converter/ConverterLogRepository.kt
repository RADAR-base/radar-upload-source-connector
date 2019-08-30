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

import org.radarbase.connect.upload.api.Log
import org.radarbase.connect.upload.api.LogLevel
import org.radarbase.connect.upload.api.UploadBackendClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant


interface LogRepository {
    fun info(logger: Logger, recordId: Long, logMessage: String)
    fun debug(logger: Logger, recordId: Long, logMessage: String)
    fun warn(logger: Logger, recordId: Long, logMessage: String)
    fun error(logger: Logger, recordId: Long, logMessage: String, exe: Exception? = null)
    fun uploadLogs(recordId: Long, isImmediate: Boolean? = false)
    fun uploadAllLogs()
}
class ConverterLogRepository(
        val uploadClient: UploadBackendClient): LogRepository {
    val logContainer = mutableListOf<Log>()

    override fun info(logger: Logger, recordId: Long, logMessage: String) {
        logger.info("IN logrepose")
        logContainer.add(Log(recordId, LogLevel.INFO, logMessage))
        logger.info(logMessage)
    }

    override fun debug(logger: Logger, recordId: Long, logMessage: String) {
        logContainer.add(Log(recordId, LogLevel.DEBUG, logMessage))
        logger.debug(logMessage)
    }

    override fun warn(logger: Logger, recordId: Long, logMessage: String) {
        logContainer.add(Log(recordId, LogLevel.WARN, logMessage))
        logger.warn(logMessage)
    }

    override fun error(logger: Logger, recordId: Long, logMessage: String, exe: Exception?) {
        logContainer.add(Log(recordId, LogLevel.ERROR, "$logMessage: ${exe?.stackTrace?.toString()}"))
        logger.error(logMessage, exe)
    }

    override fun uploadLogs(recordId: Long, isImmediate: Boolean?) {
        // upload logs of a record and remove uploaded ones on success
        logContainer.filter { it.recordId == recordId }
                .toList().toString()
    }

    override fun uploadAllLogs() {
        logger.info("TODO: Uploading all remaining logs")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ConverterLogRepository::class.java)
    }

}
