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
import java.io.InputStream

/**
 * Abstract File Pre Processor.
 */
open class FilePreProcessorFactory(
    private val preProcessors: List<FileProcessorFactory>
) : FileProcessorFactory {

    override fun matches(contents: ContentsDTO): Boolean = contents.fileName.endsWith(".csv")

    override fun createProcessor(record: RecordDTO): FileProcessorFactory.FileProcessor = FilePreProcessor(record)

    inner class FilePreProcessor(private val record: RecordDTO): FileProcessorFactory.FileProcessor {
        override fun processData(
            contents: ContentsDTO,
            inputStream: InputStream,
            timeReceived: Double,
            produce: (TopicData) -> Unit,
        ) = Unit

        override fun preProcessFile(contents: ContentsDTO, inputStream: InputStream): InputStream {
            val processor = preProcessors.find { it.matches(contents) }
                ?: throw DataProcessorNotFoundException("Could not find registered processor")

            return processor.createProcessor(record)
                .preProcessFile(contents, inputStream)
        }
    }
}
