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

import org.radarbase.connect.upload.converter.DataProcessor
import org.radarbase.connect.upload.converter.LogRepository
import org.radarbase.connect.upload.converter.TopicData
import org.radarcns.connector.upload.altoida.AltoidaMetadata
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.time.LocalDateTime
import java.time.ZoneOffset

class AltoidaMetadataDataProcessor(
        override val schemaType: String = "VERSION.csv",
        val topic: String = "connect_upload_altoida_metadata") : DataProcessor {

    override fun processData(recordId: Long, inputStream: InputStream, timeReceived: Double, logRepository: LogRepository): List<TopicData> {
        val reader = inputStream.bufferedReader()
        try {
            val version = reader.readLine()
            val convertedLine = convertVersionToRecord(version, timeReceived)
            if (convertedLine != null) {
                return listOf(convertedLine)
            }
        } catch (exe: IOException) {
            logRepository.warn(logger, recordId,"Something went wrong while processing contents of file $recordId: ${exe.message} ")
        } finally {
            logRepository.info(logger, recordId,"Closing resources of content")
            inputStream.close()
        }
        return emptyList()
    }

    private fun convertVersionToRecord(version: String, timeReceived: Double): TopicData? {
        val time = LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()?.toDouble()
        val metadata = AltoidaMetadata(
                time,
                timeReceived,
                version
        )
        return TopicData(false, topic, metadata)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AltoidaMetadataDataProcessor::class.java)
    }
}

