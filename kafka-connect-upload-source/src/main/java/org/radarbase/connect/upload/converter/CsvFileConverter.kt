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

import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.LogLevel
import org.radarbase.connect.upload.api.RecordDTO
import org.slf4j.LoggerFactory
import java.io.InputStream

abstract class CsvFileConverter(sourceType: String, val csvProcessor: CsvProcessor) : RecordConverter(sourceType) {

    override fun processData(contents: ContentsDTO, inputStream: InputStream, record: RecordDTO, timeReceived: Double): List<TopicData> {
        log(LogLevel.INFO,"Retrieved file content from record id ${record.id} and filename ${contents.fileName}")
        val convertedTopicData = mutableListOf<TopicData>()
        try {
            convertedTopicData.addAll(csvProcessor.processData(inputStream, timeReceived))
            convertedTopicData.last().endOfFileOffSet = true
        } catch (exe: Exception) {

        }
        return convertedTopicData
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CsvFileConverter::class.java)
    }
}
