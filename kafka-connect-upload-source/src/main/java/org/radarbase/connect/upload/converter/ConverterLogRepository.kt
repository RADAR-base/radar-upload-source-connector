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
import org.radarbase.connect.upload.api.RecordMetadataDTO
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
    fun uploadLogs(recordId: Long, isImmediate: Boolean? = false): RecordMetadataDTO
    fun uploadAllLogs()
}

class ConverterLogRepository(
        val uploadClient: UploadBackendClient): LogRepository {
    private val logContainer = mapOf<Long, MutableList<Log>>().withDefault { mutableListOf() }

    override fun info(logger: Logger, recordId: Long, logMessage: String) {
        logger.info("IN log info")
        logContainer.getValue(recordId).add(Log(LogLevel.INFO, logMessage))
        logger.info(logMessage)
    }

    override fun debug(logger: Logger, recordId: Long, logMessage: String) {
        logContainer.getValue(recordId).add(Log(LogLevel.DEBUG, logMessage))
        logger.debug(logMessage)
    }

    override fun warn(logger: Logger, recordId: Long, logMessage: String) {
        logContainer.getValue(recordId).add(Log(LogLevel.WARN, logMessage))
        logger.warn(logMessage)
    }

    override fun error(logger: Logger, recordId: Long, logMessage: String, exe: Exception?) {
        logContainer.getValue(recordId).add(Log(LogLevel.ERROR, "$logMessage: ${exe?.stackTrace?.toString()}"))
        logger.error(logMessage, exe)
    }

    override fun uploadLogs(recordId: Long, isImmediate: Boolean?): RecordMetadataDTO {
        val listOfLogs = logContainer.getValue(recordId)
        logger.debug("Sending record logs..")
        val logs = LogsDto().apply {
            contents = UploadSourceConnectorConfig.mapper.writeValueAsString(listOfLogs)
        }
        logger.info(UploadSourceConnectorConfig.mapper.writeValueAsString(logs.contents))
        return uploadClient.addLogs(recordId, logs)
    }

    override fun uploadAllLogs() {
        logger.info("TODO: Uploading all remaining logs")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ConverterLogRepository::class.java)
    }

}
