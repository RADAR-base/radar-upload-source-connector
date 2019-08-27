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

import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import okhttp3.ResponseBody
import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.exception.InvalidFormatException
import org.radarbase.connect.upload.api.LogLevel.*
import java.io.IOException

abstract class CsvRecordConverter(sourceType: String) : RecordConverter(sourceType) {

    override fun processData(contents: ContentsDTO, responseBody: ResponseBody, record: RecordDTO, timeReceived: Double): List<TopicData> {
        log(INFO,"Retrieved file content from record id ${record.id} and filename ${contents.fileName}")
        val inputStream = responseBody.byteStream()
        val reader = CSVReaderBuilder(inputStream.bufferedReader())
                .withCSVParser(CSVParserBuilder().withSeparator(',').build())
                .build()
        val convertedTopicData = mutableListOf<TopicData>()
        try {
            val header = reader.readNext().map { it.trim() }
            if (validateHeaderSchema(header)) {
                var line = reader.readNext()
                while (line != null && line.isNotEmpty()) {
                    val convertedLine = convertLineToRecord(header, line.asList(), timeReceived)
                    if (convertedLine != null) {
                        convertedTopicData.add(convertedLine)
                    }
                    line = reader.readNext()
                }
                convertedTopicData.last().endOfFileOffSet = true
            } else {
                throw InvalidFormatException("Csv header does not match with expected converter format")
            }
        } catch (exe: IOException) {
            log(WARN,"Something went wrong while processing contents of file ${contents.fileName}: ${exe.message} ")
        } finally {
            log(INFO,"Closing resources of content ${contents.fileName}")
            inputStream.close()
            responseBody.close()
        }
        return convertedTopicData
    }

    abstract fun validateHeaderSchema(csvHeader: List<String>): Boolean

    private fun convertLineToRecord(header: List<String>, line: List<String>, timeReceived: Double): TopicData? {

        if(line.isEmpty()) {
            log(WARN, "Empty line found ${line.toList()}")
            return null
        }

        if (header.size != line.size) {
            log(WARN, "Line size ${line.size} did not match with header size ${header.size}. Skipping this line")
            return null
        }

        if (line.any { it.isEmpty()}) {
            log(WARN,"Line with empty values found. Skipping this line")
            return null
        } else {
            return convertLineToRecord(header.zip(line).toMap(), timeReceived)
        }
    }

    abstract fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData?
}
