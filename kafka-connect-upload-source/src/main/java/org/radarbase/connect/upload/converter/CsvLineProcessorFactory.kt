/*
 *  Copyright 2019 The Hyve
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.radarbase.connect.upload.converter

import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO

interface CsvLineProcessorFactory {
    val header: List<String>

    fun matches(contents: ContentsDTO): Boolean = contents.fileName.endsWith(".csv")
    fun matches(header: List<String>): Boolean = header.containsAll(this.header)
    fun csvProcessor(record: RecordDTO, logRepository: LogRepository): CsvLineProcessor

    interface CsvLineProcessor {
        val recordLogger: RecordLogger

        fun isLineValid(
                header: List<String>,
                line: Array<String>): Boolean {
            return when {
                line.isEmpty() -> {
                    recordLogger.warn("Empty line found ${line.toList()}")
                    false
                }
                header.size != line.size -> {
                    recordLogger.warn("Line size ${line.size} does not match with header size ${header.size}. Skipping this line")
                    false
                }
                line.any { it.isEmpty() } -> {
                    recordLogger.warn("Line with empty values found. Skipping this line")
                    false
                }
                else -> true
            }
        }

        fun convertToRecord(lineValues: Map<String, String>, timeReceived: Double): FileProcessorFactory.TopicData?
    }
}

