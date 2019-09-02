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
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.exception.ConversionFailedException
import org.slf4j.LoggerFactory
import java.io.InputStream

/**
 * Abstract CSV File converter. This can be used to convert a single CSV file into TopicData.
 * Implementing classes should provide unique source-type and a compatible DataProcessor.
 */
abstract class CsvFileRecordConverter(sourceType: String, val csvProcessor: CsvProcessor) : RecordConverter(sourceType) {

    override fun processData(contents: ContentsDTO, inputStream: InputStream, record: RecordDTO, timeReceived: Double): List<TopicData> {
        val recordId = record.id!!
        logRepository.info(logger, recordId, "Retrieved file content from record id $recordId and filename ${contents.fileName}")
        val convertedTopicData = mutableListOf<TopicData>()
        try {
            convertedTopicData.addAll(csvProcessor.processData(recordId, inputStream, timeReceived, logRepository))
            convertedTopicData.last().endOfFileOffSet = true
        } catch (exe: Exception) {
            logRepository.error(logger, recordId, "Could not convert csv file ${contents.fileName}", exe)
            throw ConversionFailedException("Coult not convert csv file ${contents.fileName}", exe)
        }
        return convertedTopicData
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CsvFileRecordConverter::class.java)
    }
}
