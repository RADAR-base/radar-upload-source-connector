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
import org.radarbase.connect.upload.io.TempFile.Companion.toTempFile
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

    open fun beforeProcessing(contents: ConverterFactory.ContentsContext) = Unit

    open fun afterProcessing(contents: ConverterFactory.ContentsContext) = Unit

    override fun matches(contents: ContentsDTO): Boolean = contents.fileName.endsWith(".zip")

    override fun createProcessor(record: RecordDTO): FileProcessorFactory.FileProcessor = ZipFileProcessor(record)

    inner class ZipFileProcessor(private val record: RecordDTO): FileProcessorFactory.FileProcessor {
        override fun processData(
            context: ConverterFactory.ContentsContext,
            inputStream: InputStream,
            timeReceived: Double,
            produce: (TopicData) -> Unit
        ) {
            context.logger.info("Retrieved Zip content from filename ${context.fileName}")
            beforeProcessing(context)
            try {
                ZipInputStream(inputStream).use { zippedInput ->
                    generateSequence { zippedInput.nextEntry }
                        .ifEmpty { throw IOException("No zipped entry found from ${context.fileName}") }
                        .filter { !it.isDirectory && entryFilter(it.name.trim()) }
                        .forEach { zippedEntry ->
                            val entryName = zippedEntry.name.trim()
                            context.logger.debug("Processing entry $entryName from record ${record.id}")

                            val entryContext = ConverterFactory.ContentsContext.create(
                                record,
                                ContentsDTO(
                                    fileName = entryName,
                                    size = zippedEntry.size
                                ),
                                context.logger,
                                context.avroData,
                            )

                            val processors = entryProcessors.createProcessors(entryContext)

                            val entryStream: InputStream = object : FilterInputStream(zippedInput) {
                                @Throws(IOException::class)
                                override fun close() {
                                    context.logger.debug("Closing entry $entryName")
                                    zippedInput.closeEntry()
                                }
                            }.preProcess(processors, entryContext)

                            if (processors.size == 1) {
                                processors.first().convertDirectly(
                                    entryContext,
                                    timeReceived,
                                    entryStream,
                                    produce,
                                )
                            } else {
                                processors.convertViaTempFile(
                                    entryContext,
                                    timeReceived,
                                    entryStream,
                                    produce,
                                )
                            }
                        }
                }
            } catch (exe: IOException) {
                context.logger.error("Failed to process zipped input from record ${record.id}", exe)
                throw exe
            } catch (exe: Exception) {
                context.logger.error("Could not process record ${record.id}", exe)
                throw exe
            } finally {
                afterProcessing(context)
            }
        }


        private fun FileProcessorFactory.FileProcessor.convertDirectly(
            context: ConverterFactory.ContentsContext,
            timeReceived: Double,
            inputStream: InputStream,
            produce: (TopicData) -> Unit,
        ) {
            context.logger.debug("Processing ${context.fileName} with ${javaClass.simpleName} processor")
            processData(context, inputStream, timeReceived, produce)
        }

        private fun List<FileProcessorFactory.FileProcessor>.convertViaTempFile(
            context: ConverterFactory.ContentsContext,
            timeReceived: Double,
            inputStream: InputStream,
            produce: (TopicData) -> Unit
        ) {
            val entryName = context.fileName.replace(nonAlphaNumericRegex, "").takeLast(50)
            inputStream.toTempFile(tempDir, "record-entry-${record.id}-$entryName").use { tempFile ->
                forEach { processor ->
                    processor.convertDirectly(
                        context,
                        timeReceived,
                        tempFile.inputStream().buffered(),
                        produce,
                    )
                }
            }
        }
    }

    companion object {
        private val nonAlphaNumericRegex = "\\W+".toRegex()
    }
}
