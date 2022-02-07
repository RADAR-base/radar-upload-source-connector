/*
 *  Copyright 2019 The Hyve
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.radarbase.connect.upload.converter.xml

import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.FileProcessor
import org.radarbase.connect.upload.converter.TimeFieldParser
import org.radarbase.connect.upload.converter.TopicData
import org.w3c.dom.Element
import java.io.IOException
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Base class to process XML files with multiple possible per-node/tag processors.
 */
open class XmlProcessor(
        private val record: RecordDTO? = null,
        private val processorFactories: List<XmlNodeProcessorFactory>,
): FileProcessor {
    val fileNameSuffixes: List<String>
        get() = listOf(fileNameSuffix)

    open val fileNameSuffix: String = ".xml"

    open val timeFieldParser: TimeFieldParser = TimeFieldParser.EpochMillisParser()

    override fun processData(
            context: ConverterFactory.ContentsContext,
            inputStream: InputStream,
            produce: (TopicData) -> Unit,
    ) {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()

        if (context.contents.size == 0L) {
            throw IOException("Cannot read empty XML file ${context.fileName}")
        }

        val doc: Element = builder.parse(inputStream).documentElement
        val processors = processorFactories.filter { it.matches(context.contents) }.createNodeProcessors(context)

        processXml(doc, context, processors, "", produce)
    }

    /**
     * Recursively traverses the xml nodes and converts each node if processor (that matches node name) exists.
     */
    open fun processXml(root: Element, context: ConverterFactory.ContentsContext, contentProcessorsFactories: List<XmlNodeProcessorFactory.XmlNodeProcessor>, metadata: String, produce: (TopicData) -> Unit) {
        val children = root.childNodes
        for (i in 0 until children.length) {
            var n = children.item(i)
            if (n.hasChildNodes()) {
                n = n as Element
                val nodeName = n.nodeName
                val processor = contentProcessorsFactories.firstOrNull { it.matches(nodeName) }
                processor?.let { processor.convertToRecord(n, context.timeReceived, metadata).forEach(produce) }

                processXml(n, context, contentProcessorsFactories, metadata, produce)
            }
        }
    }


    fun time(line: Map<String, String>): Double = timeFieldParser.time(line)

    /**
     * Whether the file contents matches this xml processor.
     */
    fun matches(contents: ContentsDTO) = fileNameSuffixes.any {
        contents.fileName.endsWith(it, ignoreCase = true)
    }

    private fun List<XmlNodeProcessorFactory>.createNodeProcessors(
            context: ConverterFactory.ContentsContext,
    ): List<XmlNodeProcessorFactory.XmlNodeProcessor> {
        val processors = this.map { it.createNodeProcessor(context) }

        return processors
    }

}


