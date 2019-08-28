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
import org.radarbase.connect.upload.api.LogLevel
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.exception.ProcessorNotFoundException
import org.slf4j.LoggerFactory
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

abstract class ZipFileRecordConverter(sourceType: String, listOfDataProcessors: List<DataProcessor>) : RecordConverter(sourceType) {

    private var processors: Map<String, DataProcessor> = listOfDataProcessors.map { it.schemaType to it }.toMap()

    override fun processData(contents: ContentsDTO, inputStream: InputStream, record: RecordDTO, timeReceived: Double): List<TopicData> {
        log(LogLevel.INFO,"Retrieved file content from record id ${record.id} and filename ${contents.fileName}")
        val convertedTopicData = mutableListOf<TopicData>()
        try {
            val zippedInput = ZipInputStream(inputStream)
            var zippedEntry: ZipEntry? = null
            zippedInput.use {
                while ({ zippedEntry = zippedInput.nextEntry; zippedEntry }() != null) {
                    val entryName = zippedEntry!!.name.trim()
                    logger.debug("Processing entry $entryName from record ${record.id}")
                    convertedTopicData.addAll(processContent(object : FilterInputStream(zippedInput) {
                        @Throws(IOException::class)
                        override fun close() {
                            logger.debug("Closing entry $entryName")
                            zippedInput.closeEntry()
                        }
                    }, entryName, timeReceived))
                }
            }
            convertedTopicData.last().endOfFileOffSet = true
        } catch (exe: IOException) {
            log(LogLevel.ERROR, "Failed to process zipped input from record ${record.id}", exe)
        } catch (exe: Exception) {
            log(LogLevel.ERROR, "Could not process record ${record.id}", exe)
        }

        return convertedTopicData
    }

    private fun processContent(inputStream: InputStream, zipEntryName: String, timeReceived: Double): List<TopicData> {
        return getDataProcessor(zipEntryName).processData(inputStream, timeReceived)
    }

    private fun getDataProcessor(zipEntryName: String): DataProcessor {
        val processorKey = processors.keys.find {zipEntryName.endsWith(it)} ?: throw ProcessorNotFoundException("Could not find registered processor for zipped entry $zipEntryName")
        logger.debug("Processing $zipEntryName with $processorKey processor")
        return processors[processorKey] ?: throw throw ProcessorNotFoundException("No processor found for key $processorKey")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ZipFileRecordConverter::class.java)
    }
}
