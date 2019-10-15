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
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Abstract Zip file converter. Implementing classes should provide specific source-type and compatible
 * DataProcessors that can process each entry in the Zip file.
 */
open class ZipFileProcessorFactory(
        private val entryProcessors: List<FileProcessorFactory>,
        private val logRepository: LogRepository
) : FileProcessorFactory {
    override fun matches(filename: String): Boolean = filename.endsWith(".zip")

    override fun fileProcessor(record: RecordDTO): FileProcessorFactory.FileProcessor = ZipFileDataProcessor(record)

    inner class ZipFileDataProcessor(record: RecordDTO): AbstractFileProcessor(record, logRepository) {
        override fun processData(contents: ContentsDTO, inputStream: InputStream, timeReceived: Double): List<FileProcessorFactory.TopicData> {
            recordLogger.info("Retrieved file content from record id ${record.id} and filename ${contents.fileName}")
            return try {
                val zippedInput = ZipInputStream(inputStream)
                zippedInput.use {
                    generateSequence { it.nextEntry }
                            .ifEmpty { throw IOException("No zipped entry found from ${contents.fileName}") }
                            .flatMap { zippedEntry ->
                                val entryName = zippedEntry.name.trim()
                                recordLogger.debug("Processing entry $entryName from record $recordId")
                                val processor = entryProcessors.find { it.matches(entryName) }
                                        ?: throw DataProcessorNotFoundException("Could not find registered processor for zipped entry $entryName")

                                recordLogger.debug("Processing $entryName with ${processor.javaClass.simpleName} processor")

                                processor.fileProcessor(record)
                                        .processData(contents, object : FilterInputStream(zippedInput) {
                                            @Throws(IOException::class)
                                            override fun close() {
                                                recordLogger.debug("Closing entry $entryName")
                                                zippedInput.closeEntry()
                                            }
                                        }, timeReceived)
                                        .asSequence()
                            }
                            .toList()
                }

            } catch (exe: IOException) {
                recordLogger.error("Failed to process zipped input from record ${record.id}", exe)
                throw exe
            } catch (exe: Exception) {
                recordLogger.error("Could not process record ${record.id}", exe)
                throw exe
            }
        }
    }
}
