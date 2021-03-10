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
import org.radarbase.connect.upload.exception.DataProcessorNotFoundException
import org.slf4j.LoggerFactory
import java.io.InputStream

/**
 * Abstract File Pre Processor.
 */
open class FilePreProcessorFactory(
        private val preProcessors: List<FileProcessorFactory>,
        private val logRepository: LogRepository) : FileProcessorFactory {

    override fun matches(contents: ContentsDTO): Boolean = contents.fileName.endsWith(".csv")

    override fun createProcessor(record: RecordDTO): FileProcessorFactory.FileProcessor = FilePreProcessor(record)

    inner class FilePreProcessor(private val record: RecordDTO): FileProcessorFactory.FileProcessor {
        private val recordLogger = logRepository.createLogger(logger, record.id!!)

        override fun processData(contents: ContentsDTO, inputStream: InputStream, timeReceived: Double): List<TopicData> {
            return emptyList()
        }

        override fun preProcessFile(contents: ContentsDTO, inputStream: InputStream): InputStream {
            val processor = preProcessors.find { it.matches(contents) }
                    ?: throw DataProcessorNotFoundException("Could not find registered processor")

            return processor.createProcessor(record)
                    .preProcessFile(contents, inputStream)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FilePreProcessor::class.java)
    }
}
