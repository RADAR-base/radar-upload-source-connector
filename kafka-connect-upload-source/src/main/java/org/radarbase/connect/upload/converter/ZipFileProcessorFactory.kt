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
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Paths
import java.util.zip.ZipInputStream

/**
 * Abstract Zip file converter. Implementing classes should provide specific source-type and compatible
 * DataProcessors that can process each entry in the Zip file.
 */
open class ZipFileProcessorFactory(
    sourceType: String,
    zipEntryProcessors: List<FileProcessorFactory>,
    allowUnmappedFiles: Boolean = false
) : FileProcessorFactory {
    private val delegatingProcessor = DelegatingProcessor(
        processorFactories = zipEntryProcessors,
        tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "upload-connector", "$sourceType-zip-cache"),
        generateTempFilePrefix = { context ->
            val safeEntryName = context.fileName
                .replace(nonAlphaNumericRegex, "")
                .takeLast(50)
            "record-entry-${context.id}-$safeEntryName-"
        },
        allowUnmappedFiles = allowUnmappedFiles,
    )

    open fun entryFilter(name: String): Boolean = true

    open fun beforeProcessing(contents: ConverterFactory.ContentsContext) = Unit

    open fun afterProcessing(contents: ConverterFactory.ContentsContext) = Unit

    override fun matches(contents: ContentsDTO): Boolean = contents.fileName.endsWith(".zip")

    override fun createProcessor(record: RecordDTO): FileProcessorFactory.FileProcessor = ZipFileProcessor(record)

    inner class ZipFileProcessor(private val record: RecordDTO): FileProcessorFactory.FileProcessor {
        override fun processData(
            context: ConverterFactory.ContentsContext,
            inputStream: InputStream,
            produce: (TopicData) -> Unit
        ) {
            context.logger.info("Retrieved Zip content from filename ${context.fileName}")
            beforeProcessing(context)
            try {
                ZipInputStream(inputStream).use { zipInputStream ->
                    generateSequence { zipInputStream.nextEntry }
                        .ifEmpty { throw IOException("No zipped entry found from ${context.fileName}") }
                        .filter { !it.isDirectory && entryFilter(it.name.trim()) }
                        .forEach { zipEntry ->
                            delegatingProcessor.processData(
                                context = context.copy(
                                    contents = ContentsDTO(
                                        fileName = zipEntry.name.trim(),
                                        size = zipEntry.size
                                    ),
                                ),
                                inputStream = object : FilterInputStream(zipInputStream) {
                                    @Throws(IOException::class)
                                    override fun close() {
                                        context.logger.debug("Closing entry ${context.fileName}")
                                        zipInputStream.closeEntry()
                                    }
                                },
                                produce,
                            )
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
    }

    companion object {
        private val nonAlphaNumericRegex = "\\W+".toRegex()
    }
}
