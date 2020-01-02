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
        private val logRepository: LogRepository) : FileProcessorFactory {
    open fun entryFilter(name: String): Boolean = true

    open fun beforeProcessing(contents: ContentsDTO) = Unit

    open fun afterProcessing(contents: ContentsDTO) = Unit

    override fun matches(contents: ContentsDTO): Boolean = contents.fileName.endsWith(".zip")

    override fun createProcessor(record: RecordDTO): FileProcessorFactory.FileProcessor = ZipFileProcessor(record)

    inner class ZipFileProcessor(private val record: RecordDTO): FileProcessorFactory.FileProcessor {
        private val recordLogger = logRepository.createLogger(logger, record.id!!)
        override fun processData(contents: ContentsDTO, inputStream: InputStream, timeReceived: Double): List<TopicData> {
            recordLogger.info("Retrieved file content from record id ${record.id} and filename ${contents.fileName}")
            beforeProcessing(contents)
            return try {
                ZipInputStream(inputStream).use { zippedInput ->
                    generateSequence { zippedInput.nextEntry }
                            .ifEmpty { throw IOException("No zipped entry found from ${contents.fileName}") }
                            .filter { !it.isDirectory && entryFilter(it.name.trim()) }
                            .flatMap { zippedEntry ->
                                val entryName = zippedEntry.name.trim()
                                recordLogger.debug("Processing entry $entryName from record ${record.id}")

                                val entryContents = ContentsDTO(fileName = entryName)

                                val processor = entryProcessors.find { it.matches(entryContents) }
                                        ?: throw DataProcessorNotFoundException("Could not find registered processor for zipped entry $entryName")

                                recordLogger.debug("Processing $entryName with ${processor.javaClass.simpleName} processor")

                                processor.createProcessor(record)
                                        .processData(entryContents, object : FilterInputStream(zippedInput) {
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
            } finally {
                afterProcessing(contents)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ZipFileProcessor::class.java)
    }
}
