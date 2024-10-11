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

package org.radarbase.connect.upload.converter.altoidav2.summary

import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder
import com.opencsv.CSVWriter
import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.FilePreProcessor
import org.radarbase.connect.upload.converter.FilePreProcessorFactory
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * AltoidaSummaryCsvPreProcesor allows the preprocessing of the export.csv InputStream
 * before the data is converted into kafka records. This is needed because many export.csv files
 * have incorrect headers (some header fields are empty or incorrectly matched with the values),
 * even when the content/values are correct. This replaces the whole header and replaces it with
 * the correct expected header.
 */
class AltoidaSummaryCsvPreProcessorFactory : FilePreProcessorFactory {
    override fun matches(contents: ContentsDTO): Boolean = contents.fileName.endsWith("export.csv")

    override fun createPreProcessor(record: RecordDTO): FilePreProcessor = AltoidaSummaryCsvPreProcessor()

    private inner class AltoidaSummaryCsvPreProcessor : FilePreProcessor {
        override fun preProcessFile(
            context: ConverterFactory.ContentsContext,
            inputStream: InputStream,
        ): InputStream {
            logger.info("Converting input stream..")
            return inputStream.bufferedReader().toCsvReader().use { reader ->
                var header = reader.readNext()?.map { it }
                    .takeIf { !it.isNullOrEmpty() }
                    .orEmpty()
                    .filter { h -> h.isNotEmpty() }

                // if (header.size < fileHeader.size) header = fileHeader
                val line = reader.readNext()
                val outputStream = ByteArrayOutputStream()
                val writer = CSVWriter(outputStream.writer(StandardCharsets.UTF_8))

                writer.writeNext(header.toTypedArray())
                writer.writeNext(line)
                writer.flush()
                writer.close()
                outputStream.flush()
                outputStream.close()

                ByteArrayInputStream(outputStream.toByteArray())
            }
        }

        private fun BufferedReader.toCsvReader(): CSVReader = CSVReaderBuilder(this)
            .withCSVParser(CSVParserBuilder().withSeparator(',').build())
            .build()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AltoidaSummaryCsvPreProcessorFactory::class.java)

        /** Expected file header list (uppercase). */
        val fileHeader = listOf(
            "LABEL",
            "TIMESTAMP",
            "CLASS",
            "NMI",
            "DOMAINPERCENTILE_PERCEPTUALMOTORCOORDINATION",
            "DOMAINPERCENTILE_COMPLEXATTENTION",
            "DOMAINPERCENTILE_COGNITIVEPROCESSINGSPEED",
            "DOMAINPERCENTILE_INHIBITION",
            "DOMAINPERCENTILE_FLEXIBILITY",
            "DOMAINPERCENTILE_VISUALPERCEPTION",
            "DOMAINPERCENTILE_PLANNING",
            "DOMAINPERCENTILE_PROSPECTIVEMEMORY",
            "DOMAINPERCENTILE_EYEMOVEMENT",
            "DOMAINPERCENTILE_SPEECH",
            "DOMAINPERCENTILE_SPATIALMEMORY",
        )
    }
}
