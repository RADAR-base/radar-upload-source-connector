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
import org.radarbase.connect.upload.converter.TopicData
import org.radarcns.connector.upload.altoida.AltoidaMetadata
import java.io.IOException
import java.io.InputStream
import java.time.LocalDateTime
import java.time.ZoneOffset

class AltoidaMetadataDataProcessor(
        override val schemaType: String = "VERSION.csv",
        val topic: String = "connect_upload_altoida_metadata") : DataProcessor {

    override fun processData(inputStream: InputStream, timeReceived: Double): List<TopicData> {
        val reader = inputStream.bufferedReader()
        try {
            val version = reader.readLine()
            val convertedLine = convertVersionToRecord(version, timeReceived)
            if (convertedLine != null) {
                return listOf(convertedLine)
            }
        } catch (exe: IOException) {
//            log(LogLevel.WARN,"Something went wrong while processing contents of file ${contents.fileName}: ${exe.message} ")
        } finally {
//            log(LogLevel.INFO,"Closing resources of content ${contents.fileName}")
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
}
