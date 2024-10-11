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
import org.w3c.dom.NodeList

/**
 * Processor for processing single nodes/elements of an XML file.
 */
abstract class XmlNodeProcessorFactory {
    open val fileNameSuffixes: List<String>
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

    open fun convertToSingleRecord(
        root: Element,
        timeReceived: Double,
        assessmentName: String? = "",
    ): TopicData? = null

    open fun nodeConversions(
        root: Element,
        timeReceived: Double,
        assessmentName: String?,
    ): Sequence<TopicData> = root.childNodes
        .asSequence()
        .filter { it.hasAttributes() && it is Element }
        .mapNotNull { convertToSingleRecord(it as Element, timeReceived, assessmentName) }

    fun createNodeProcessor(
        context: ConverterFactory.ContentsContext,
    ): XmlNodeProcessor = XmlNodeProcessor(context, nodeName) { l, t, a ->
        nodeConversions(l, t, a)
    }

    protected fun Element.attributeToTime(name: String) = timeFieldParser.timeFromString(getAttribute(name))

    class XmlNodeProcessor(
        val context: ConverterFactory.ContentsContext,
        private val nodeName: String,
        private val conversion: (
            node: Element,
            timeReceived: Double,
            assessmentName: String,
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
            assessmentName: String,
        ) = conversion(node, timeReceived, assessmentName)
    }

    companion object {
        private fun Element.childNodeOrNull(tag: String): Node? = getElementsByTagName(tag).item(0)

        /**
         * Get the first child element with the given tag sequence.
         * @throws IllegalArgumentException if no tag of the given sequence can be found.
         */
        // NOTE: This is assuming tags in each tree are unique
        // The tagList represents the branch leading to required element
        fun Element.child(
            vararg tags: String,
        ): Element =
            tags.fold(this) { node, tag ->
                requireNotNull(node.childOrNull(tag)) {
                    "Missing tag $tag of $tags in XML document"
                }
            }

        /**
         * Get the text content from a child tag.
         * If no child element is present, return [default] value.
         * Only the first child element with given tag will be used for finding the text.
         */
        fun Element.childValue(
            tag: String,
            default: String = "",
        ): String = childNodeOrNull(tag)?.textContent ?: default

        /**
         * Get the child of the element by tag. Returns `null` if no element by that tag can be
         * found. Only the first child with given tag will be returned.
         */
        fun Element.childOrNull(tag: String): Element? =
            childNodeOrNull(tag) as? Element

        /**
         * Get the attribute value with given [name]. If the current element is null or if the
         * attribute is not present, this will return an empty string.
         */
        fun Element?.attribute(
            name: String,
            default: String = "",
        ): String = this?.getAttribute(name) ?: default

        /**
         * Iterates over a node list as a sequence.
         */
        fun NodeList.asSequence(): Sequence<Node> {
            return (0 until length)
                .asSequence()
                .map { i -> item(i) }
        }
    }
}
