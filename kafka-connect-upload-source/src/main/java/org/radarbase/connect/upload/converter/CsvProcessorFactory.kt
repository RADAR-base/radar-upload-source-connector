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

import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder
import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.exception.InvalidFormatException
import java.io.IOException
import java.io.InputStream

class CsvProcessorFactory(private val processorFactories: List<CsvLineProcessorFactory>, private val logRepository: LogRepository): FileProcessorFactory {
    override fun matches(filename: String) = processorFactories.any { it.matches(filename) }

    override fun fileProcessor(record: RecordDTO): FileProcessorFactory.FileProcessor {
        return CsvProcessor(processorFactories, record, logRepository)
    }

    class CsvProcessor(
            private val processorFactories: List<CsvLineProcessorFactory>,
            record: RecordDTO,
            private val logRepository: LogRepository): AbstractFileProcessor(record, logRepository) {

        override fun processData(contents: ContentsDTO, inputStream: InputStream, timeReceived: Double): List<FileProcessorFactory.TopicData> {
            return try {
                convertLines(contents, inputStream, timeReceived)
            } catch (exe: IOException) {
                recordLogger.error("Something went wrong while processing a contents of record ${recordId}: ${exe.message} ")
                throw exe
            } finally {
                recordLogger.info("Closing resources of content")
            }
        }

        private fun convertLines(
                contents: ContentsDTO,
                inputStream: InputStream,
                timeReceived: Double): List<FileProcessorFactory.TopicData> = readCsv(inputStream) { reader ->
            val header = reader.readNext().map { it.trim() }
            val processorFactory = processorFactories
                    .find { it.matches(contents.fileName) && it.matches(header) }
                    ?: throw InvalidFormatException("In record $recordId, cannot find CSV processor that matches header $header")

            val processor = processorFactory.csvProcessor(record, logRepository)

            generateSequence { reader.readNext() }
                    .filter { processor.isLineValid(header, it) }
                    .mapNotNull { processor.convertToRecord(header.zip(it).toMap(), timeReceived) }
                    .toList()
        }

        private fun <T> readCsv(
                inputStream: InputStream,
                action: (reader: CSVReader) -> T
        ): T = CSVReaderBuilder(inputStream.bufferedReader())
                .withCSVParser(CSVParserBuilder().withSeparator(',').build())
                .build()
                .use { reader -> action(reader) }
    }
}
