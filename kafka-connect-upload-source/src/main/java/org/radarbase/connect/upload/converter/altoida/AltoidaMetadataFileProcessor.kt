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
import org.radarbase.connect.upload.converter.TopicData
import org.radarcns.connector.upload.altoida.AltoidaMetadata
import org.slf4j.LoggerFactory
import java.io.InputStream

class AltoidaMetadataFileProcessor(
        private val logRepository: LogRepository) : FileProcessorFactory {
    private val topic = "connect_upload_altoida_metadata"

    override fun matches(contents: ContentsDTO): Boolean = contents.fileName.endsWith("VERSION.csv")

    override fun createProcessor(record: RecordDTO): FileProcessorFactory.FileProcessor = AltoidaMetadataProcessor()

    private inner class AltoidaMetadataProcessor : FileProcessorFactory.FileProcessor {
        override fun processData(
            contents: ContentsDTO,
            inputStream: InputStream,
            timeReceived: Double,
            produce: (TopicData) -> Unit
        ) {
            val version = inputStream.bufferedReader().use { it.readLine() }
            produce(TopicData(
                topic,
                AltoidaMetadata(
                    System.currentTimeMillis() / 1000.0,
                    timeReceived,
                    version,
                ),
            ))
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AltoidaMetadataFileProcessor::class.java)
    }
}
