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

import org.radarbase.connect.upload.UploadSourceConnectorConfig
import org.radarbase.connect.upload.api.LogsDto
import org.radarbase.connect.upload.api.UploadBackendClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

enum class LogLevel {
    INFO, DEBUG, WARN, ERROR
}

data class Log(
        var logLevel: LogLevel,
        var message: String
)

interface LogRepository {
    fun info(logger: Logger, recordId: Long, logMessage: String)
    fun debug(logger: Logger, recordId: Long, logMessage: String)
    fun warn(logger: Logger, recordId: Long, logMessage: String)
    fun error(logger: Logger, recordId: Long, logMessage: String, exe: Exception? = null)
    fun uploadLogs(recordId: Long)
    fun uploadAllLogs()
}

class ConverterLogRepository(
        val uploadClient: UploadBackendClient): LogRepository {
    private val logContainer = mutableMapOf<Long, MutableList<Log>>()

    private fun get(recordId: Long): MutableList<Log> =
            logContainer.getOrPut(recordId, { mutableListOf() })


    override fun info(logger: Logger, recordId: Long, logMessage: String) {
        get(recordId).add(Log(LogLevel.INFO, logMessage))
        logger.info(logMessage)
    }

    override fun debug(logger: Logger, recordId: Long, logMessage: String) {
        get(recordId).add(Log(LogLevel.DEBUG, logMessage))
        logger.debug(logMessage)
    }

    override fun warn(logger: Logger, recordId: Long, logMessage: String) {
        get(recordId).add(Log(LogLevel.WARN, logMessage))
        logger.warn(logMessage)
    }

    override fun error(logger: Logger, recordId: Long, logMessage: String, exe: Exception?) {
        get(recordId).add(Log(LogLevel.ERROR, "$logMessage: ${exe?.stackTrace?.toString()}"))
        logger.error(logMessage, exe)
    }

    override fun uploadLogs(recordId: Long) {
        val listOfLogs = logContainer.getValue(recordId)

        if (listOfLogs.isNotEmpty()) {
            logger.info("Sending record $recordId logs...")
            val logs = LogsDto().apply {
                contents = UploadSourceConnectorConfig.mapper.writeValueAsString(listOfLogs)
            }
            logger.info(UploadSourceConnectorConfig.mapper.writeValueAsString(logs.contents))
            uploadClient.addLogs(recordId, logs)
            logContainer.remove(recordId)
        }

    }

    override fun uploadAllLogs() {
        logger.info("Uploading all remaining logs")
        if (logContainer.isNotEmpty()) {
            logContainer.map { entry -> uploadLogs(entry.key) }
        } else {
            logger.info("All record logs are uploaded")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ConverterLogRepository::class.java)
    }

}
