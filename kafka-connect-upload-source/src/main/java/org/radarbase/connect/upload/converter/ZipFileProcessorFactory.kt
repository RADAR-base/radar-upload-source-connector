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
import org.radarbase.connect.upload.converter.RecordConverter.Companion.createProcessors
import org.radarbase.connect.upload.converter.RecordConverter.Companion.preProcess
import org.radarbase.connect.upload.exception.DataProcessorNotFoundException
import org.slf4j.LoggerFactory
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipInputStream

/**
 * Abstract Zip file converter. Implementing classes should provide specific source-type and compatible
 * DataProcessors that can process each entry in the Zip file.
 */
open class ZipFileProcessorFactory(
    sourceType: String,
    private val entryProcessors: List<FileProcessorFactory>,
    private val logRepository: LogRepository,
) : FileProcessorFactory {
    private val tempDir: Path = Paths.get(System.getProperty("java.io.tmpdir"), "upload-connector", "$sourceType-zip-cache")

    init {
        if (Files.exists(tempDir)) {
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach(Files::delete)
        }
        Files.createDirectories(tempDir)
    }


    open fun entryFilter(name: String): Boolean = true

    open fun beforeProcessing(contents: ContentsDTO) = Unit

    open fun afterProcessing(contents: ContentsDTO) = Unit

    override fun matches(contents: ContentsDTO): Boolean = contents.fileName.endsWith(".zip")

    override fun createProcessor(record: RecordDTO): FileProcessorFactory.FileProcessor = ZipFileProcessor(record)

    inner class ZipFileProcessor(private val record: RecordDTO): FileProcessorFactory.FileProcessor {
        private val recordLogger = logRepository.createLogger(logger, record.id!!)

        override fun processData(
            contents: ContentsDTO,
            inputStream: InputStream,
            timeReceived: Double,
            produce: (TopicData) -> Unit,
        ) {
            recordLogger.info("Retrieved file content from record id ${record.id} and filename ${contents.fileName}")
            beforeProcessing(contents)
            try {
                ZipInputStream(inputStream).use { zippedInput ->
                    generateSequence { zippedInput.nextEntry }
                        .ifEmpty { throw IOException("No zipped entry found from ${contents.fileName}") }
                        .filter { !it.isDirectory && entryFilter(it.name.trim()) }
                        .forEach { zippedEntry ->
                            val entryName = zippedEntry.name.trim()
                            recordLogger.debug("Processing entry $entryName from record ${record.id}")

                            val entryContents = ContentsDTO(
                                fileName = entryName,
                                size = zippedEntry.size
                            )

                            val processors = entryProcessors.createProcessors(record, entryContents)

                            val entryStream: InputStream = object : FilterInputStream(zippedInput) {
                                @Throws(IOException::class)
                                override fun close() {
                                    recordLogger.debug("Closing entry $entryName")
                                    zippedInput.closeEntry()
                                }
                            }.preProcess(processors, contents)

                            if (processors.size == 1) {
                                processors.first().convertDirectly(
                                    entryContents,
                                    timeReceived,
                                    entryStream,
                                    produce,
                                )
                            } else {
                                processors.convertViaTempFile(
                                    entryContents,
                                    timeReceived,
                                    entryStream,
                                    produce,
                                )
                            }
                        }
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


        private fun FileProcessorFactory.FileProcessor.convertDirectly(
            contents: ContentsDTO,
            timeReceived: Double,
            inputStream: InputStream,
            produce: (TopicData) -> Unit,
        ) {
            recordLogger.debug("Processing ${contents.fileName} with ${javaClass.simpleName} processor")
            processData(contents, inputStream, timeReceived, produce)
        }

        private fun List<FileProcessorFactory.FileProcessor>.convertViaTempFile(
            contents: ContentsDTO,
            timeReceived: Double,
            inputStream: InputStream,
            produce: (TopicData) -> Unit
        ) {
            val entryName = contents.fileName.replace(nonAlphaNumericRegex, "").takeLast(50)
            val tempFile = Files.createTempFile(tempDir, "record-entry-${record.id}-$entryName", ".bin")
            try {
                inputStream.copyTo(Files.newOutputStream(tempFile))
                forEach { processor ->
                    processor.convertDirectly(
                        contents,
                        timeReceived,
                        Files.newInputStream(tempFile).buffered(),
                        produce,
                    )
                }
            } finally {
                Files.delete(tempFile)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ZipFileProcessor::class.java)
        private val nonAlphaNumericRegex = "\\W+".toRegex()
    }
}
