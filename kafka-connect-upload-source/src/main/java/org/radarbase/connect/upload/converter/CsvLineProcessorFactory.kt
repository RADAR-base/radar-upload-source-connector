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

/**
 * Processor for processing single lines of CSV file.
 */
interface CsvLineProcessorFactory {
    /**
     * Whether the given file is allowed to be empty. It will not be used
     * in that case.
     */
    val optional: Boolean

    /**
     * Whether this factory must match the header of a CSV file if the file name matched.
     */
    val headerMustMatch: Boolean

    /** Upper case header list. */
    val header: List<String>

    val fileNameSuffixes: List<String>
        get() = listOf(".csv")

    /**
     * Whether the file contents matches this CSV line processor.
     */
    fun matches(contents: ContentsDTO) = fileNameSuffixes.any {
        contents.fileName.endsWith(it, ignoreCase = true)
    }

    /**
     * Whether the header matches this CSV line processor. The provided header must be in upper
     * case.
     */
    fun matches(header: List<String>) = header.containsAll(this.header)

    /**
     * Create a line processor for given record.
     */
    fun createLineProcessor(record: RecordDTO, logRepository: LogRepository): CsvLineProcessor

    interface CsvLineProcessor {
        val recordLogger: RecordLogger

        /**
         * Whether the given line is valid. If not, the line will be discarded. An error can be
         * thrown to mark the entire file invalid.
         */
        fun isLineValid(
                header: List<String>,
                line: Array<String>): Boolean {
            return when {
                line.isEmpty() -> {
                    recordLogger.warn("Empty line found ${line.toList()}")
                    false
                }
                header.size != line.size -> {
                    recordLogger.warn("Line size ${line.size} does not match with header size ${header.size}")
                    false
                }
                line.any { it.isEmpty() } -> {
                    recordLogger.warn("Line with empty values found. Skipping this line")
                    false
                }
                else -> true
            }
        }

        /**
         * Convert a line from csv to one or more records
         */
        fun convertToRecord(lineValues: Map<String, String>, timeReceived: Double): List<TopicData>?
    }
}

