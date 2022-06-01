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
import org.w3c.dom.Node

/**
 * Processor for processing single nodes/elements of an XML file.
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

    abstract fun convertToSingleRecord(root: Element, timeReceived: Double, assessmentName: String? = ""): TopicData?

    open fun nodeConversions(
            root: Element,
            timeReceived: Double,
            assessmentName: String?
    ): Sequence<TopicData> {
        val events = root.childNodes
        return ((0 until events.length)
                .asSequence()
                .filter { i -> events.item(i).hasAttributes() }
                .mapNotNull { i -> convertToSingleRecord(events.item(i) as Element, timeReceived, assessmentName) })
    }

    fun createNodeProcessor(
            context: ConverterFactory.ContentsContext
    ): XmlNodeProcessor {
        return XmlNodeProcessor(context, nodeName) { l, t, a -> nodeConversions(l, t, a) }
    }

    class XmlNodeProcessor(
            val context: ConverterFactory.ContentsContext,
            private val nodeName: String,
            private val conversion: (
                node: Element,
                timeReceived: Double,
                assessmentName: String?,
            ) -> Sequence<TopicData>,
    ) {

        /**
         * Whether the node name matches this processor.
         */
        fun matches(nodeName: String): Boolean = nodeName == this.nodeName

        /**
         * Convert a node from XML to one or more records
         */
        fun convertToRecord(
            node: Element,
            timeReceived: Double,
            assessmentName: String?
        ): Sequence<TopicData> = conversion(node, timeReceived, assessmentName)
    }

    companion object {
        fun getFirstElementByTagName(root: Element, tag: String): Node? {
            val elements = root.getElementsByTagName(tag)
            return elements.item(0)
        }

        fun getTagValue(root: Element, tag: String): String {
            val element = getFirstElementByTagName(root, tag)
            return if (element != null) element.textContent else ""
        }

        fun getAttributeValue(root: Element, tag: String, attribute: String): String {
            val element = (getFirstElementByTagName(root, tag) as Element?)
            return if  (element != null) element.getAttribute(attribute) else ""
        }

        fun getAttributeValueFromElement(element: Element, attribute: String): String = element.getAttribute(attribute)

        fun getSingleElementFromTagList(root: Element, tagList: List<String>): Element {
            // NOTE: This is assuming tags in each tree are unique
            // The tagList represents the branch leading to required element

            var currentElement: Element = root
            tagList.forEach { tag -> currentElement = currentElement.getElementsByTagName(tag).item(0) as Element }

            return currentElement
        }
    }
}

