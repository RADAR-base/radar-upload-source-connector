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
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.util.*

class CsvProcessorFactory(
        private val processorFactories: List<CsvLineProcessorFactory>,
        private val logRepository: LogRepository
): FileProcessorFactory {
    override fun matches(contents: ContentsDTO) = processorFactories.any { it.matches(contents) }

    override fun createProcessor(record: RecordDTO) = CsvProcessor(record)

    inner class CsvProcessor(private val record: RecordDTO): FileProcessorFactory.FileProcessor {
        private val recordLogger = logRepository.createLogger(logger, record.id!!)

        override fun processData(contents: ContentsDTO, inputStream: InputStream, timeReceived: Double): List<FileProcessorFactory.TopicData> {
            return try {
                convertLines(contents, inputStream, timeReceived)
            } catch (exe: IOException) {
                recordLogger.error("Something went wrong while processing a contents of record ${record.id}: ${exe.message} ")
                throw exe
            } finally {
                recordLogger.info("Closing resources of content")
            }
        }

        private fun convertLines(
                contents: ContentsDTO,
                inputStream: InputStream,
                timeReceived: Double): List<FileProcessorFactory.TopicData> = readCsv(inputStream) { reader ->
            val header = reader.readNext().map { it.trim().toUpperCase(Locale.US) }
            val processorFactory = processorFactories
                    .find { it.matches(contents) && it.matches(header) }
                    ?: throw InvalidFormatException("In record ${record.id}, cannot find CSV processor that matches header $header")

            val processor = processorFactory.createLineProcessor(record, logRepository)

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

    companion object {
        private val logger = LoggerFactory.getLogger(CsvProcessor::class.java)
    }
}
