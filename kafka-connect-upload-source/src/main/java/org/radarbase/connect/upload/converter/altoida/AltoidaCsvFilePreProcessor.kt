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

import com.opencsv.*
import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarbase.connect.upload.converter.LogRepository
import org.radarbase.connect.upload.converter.TopicData
import org.slf4j.LoggerFactory
import java.io.*

class AltoidaCsvFilePreProcessor(
        private val logRepository: LogRepository) : FileProcessorFactory {
    override fun matches(contents: ContentsDTO): Boolean = contents.fileName.endsWith("export.csv")

    override fun createProcessor(record: RecordDTO): FileProcessorFactory.FileProcessor = AltoidaCsvPreProcessor(record)

    private inner class AltoidaCsvPreProcessor(private val record: RecordDTO) : FileProcessorFactory.FileProcessor {
        override fun processData(contents: ContentsDTO, inputStream: InputStream, timeReceived: Double): List<TopicData> {
            return emptyList()
        }

        override fun preProcessFile(contents: ContentsDTO, inputStream: InputStream): InputStream {
            logger.info("Converting input stream..")

            return inputStream.bufferedReader().toCsvReader().use { reader ->
                var header = reader.readNext()?.map { it }
                val line = reader.readNext()

                val fstream = File.createTempFile("export", ".csv")
                val outputStream = FileOutputStream(fstream)
                outputStream.bufferedWriter()
                val writer = outputStream.bufferedWriter().toCsvWriter()
                if (writer != null) {
                    writer.writeNext(header?.toTypedArray())
                    writer.writeNext(line)
                    writer.close()
                }
                outputStream.flush()
                outputStream.close()
                return fstream.inputStream()
            }
        }

        protected open fun BufferedReader.toCsvReader(): CSVReader = CSVReaderBuilder(this)
                .withCSVParser(CSVParserBuilder().withSeparator(',').build())
                .build()

        protected open fun BufferedWriter.toCsvWriter(): ICSVWriter? = CSVWriterBuilder(this).build()

    }

    companion object {
        private val logger = LoggerFactory.getLogger(AltoidaCsvFilePreProcessor::class.java)
    }
}
