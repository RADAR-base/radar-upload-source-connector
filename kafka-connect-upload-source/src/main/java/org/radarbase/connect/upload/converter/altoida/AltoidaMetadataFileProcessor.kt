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

package org.radarbase.connect.upload.converter.altoida

import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarbase.connect.upload.converter.LogRepository
import org.radarcns.connector.upload.altoida.AltoidaMetadata
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.time.Instant

class AltoidaMetadataFileProcessor(
        private val logRepository: LogRepository) : FileProcessorFactory {
    private val topic = "connect_upload_altoida_metadata"

    override fun matches(contents: ContentsDTO): Boolean = contents.fileName == "VERSION.csv"

    override fun createProcessor(record: RecordDTO): FileProcessorFactory.FileProcessor = AltoidaMetadataProcessor(record)

    private inner class AltoidaMetadataProcessor(private val record: RecordDTO) : FileProcessorFactory.FileProcessor {
        val recordLogger = logRepository.createLogger(logger, record.id!!)

        override fun processData(contents: ContentsDTO, inputStream: InputStream, timeReceived: Double): List<FileProcessorFactory.TopicData> {
            val version = try {
                inputStream.bufferedReader().use { reader ->
                    reader.readLine()
                }
            } catch (exe: IOException) {
                recordLogger.warn("Something went wrong while processing contents of file ${record.id}: ${exe.message}")
                return emptyList()
            }

            return convertVersionToRecord(version, timeReceived)
                    ?.let { listOf(it) }
                    ?: emptyList()
        }

        private fun convertVersionToRecord(version: String, timeReceived: Double): FileProcessorFactory.TopicData? {
            val time = Instant.now().toEpochMilli() / 1000.0
            val metadata = AltoidaMetadata(
                    time,
                    timeReceived,
                    version)

            return FileProcessorFactory.TopicData(topic, metadata)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AltoidaMetadataFileProcessor::class.java)
    }
}