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
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.TimeFieldParser
import org.radarbase.connect.upload.converter.TopicData
import org.w3c.dom.Element

/**
 * Processor for processing single lines of CSV file.
 */
abstract class XmlNodeProcessorFactory {
    val fileNameSuffixes: List<String>
        get() = listOf(".xml")

    open val fileNameSuffix: String = ".xml"

    open val timeFieldParser: TimeFieldParser = TimeFieldParser.EpochMillisParser()

    /** Node name the processor recognises and can convert. */
    open val nodeName: String = "altoida_data"

    /** Upper case header list. */
    open val header: List<String> = listOf("")

    fun time(line: Map<String, String>): Double = timeFieldParser.time(line)

    /**
     * Whether the file contents matches this XML processor.
     */
    fun matches(contents: ContentsDTO) = fileNameSuffixes.any {
        contents.fileName.endsWith(it, ignoreCase = true)
    }

    fun getTagValue(root: Element, tag: String): String = root.getElementsByTagName(tag).item(0).getTextContent()

    fun getAttributeValue(root: Element, tag: String, attribute: String): String = (root.getElementsByTagName(tag).item(0) as Element).getAttribute(attribute)

    fun getAttributeValueFromElement(element: Element, attribute: String): String = element.getAttribute(attribute)

    fun getSingleElementFromTagList(root: Element, tagList: List<String>): Element {
        // NOTE: This is assuming tags in each tree are unique
        // The tagList represents the branch leading to required element

        var currentElement: Element = root
        tagList.forEach { tag -> currentElement = currentElement.getElementsByTagName(tag).item(0) as Element }

        return currentElement
    }

    abstract fun convertToSingleRecord(root: Element, timeReceived: Double, assessmentName: String? = ""): TopicData?

    open fun nodeConversions(
            root: Element,
            timeReceived: Double
    ): Sequence<TopicData> {
        val events = root.childNodes
        return ((0 until events.length)
                .asSequence()
                .filter { i -> events.item(i).hasAttributes() }
                .mapNotNull { i -> convertToSingleRecord(events.item(i) as Element, timeReceived) })
    }

    fun createNodeProcessor(
            context: ConverterFactory.ContentsContext
    ): XmlNodeProcessor {
        return XmlNodeProcessor(context, nodeName) { l, t -> nodeConversions(l, t) }
    }

    class XmlNodeProcessor(
            val context: ConverterFactory.ContentsContext,
            val nodeName: String,
            private val conversion: (node: Element, timeReceived: Double) -> Sequence<TopicData>,
    ) {

        /**
         * Whether the node name matches this processor.
         */
        fun matches(nodeName: String): Boolean = nodeName.equals(this.nodeName)

        /**
         * Convert a node from XML to one or more records
         */
        fun convertToRecord(
            node: Element,
            timeReceived: Double,
        ): Sequence<TopicData> {
            return conversion(node, timeReceived)
        }
    }
}

