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
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * Simple Processor for one line to one record of a single topic conversion.
 */
open class StatelessXmlLineProcessor {
    val fileNameSuffixes: List<String>
        get() = listOf(fileNameSuffix)

    open val fileNameSuffix: String = ".xml"

    open val timeFieldParser: TimeFieldParser = TimeFieldParser.EpochMillisParser()

    fun time(line: Map<String, String>): Double = timeFieldParser.time(line)

    open fun convertToSingleRecord(
            root: Element,
        timeReceived: Double
    ): TopicData? = null

    open fun convert(
            root: Element,
            timeReceived: Double
    ): Sequence<TopicData> {
        val conversion = convertToSingleRecord(root, timeReceived)
        return if (conversion != null) sequenceOf(conversion) else emptySequence()
    }

    /**
     * Whether the file contents matches this CSV line processor.
     */
    fun matches(contents: ContentsDTO) = fileNameSuffixes.any {
        contents.fileName.endsWith(it, ignoreCase = true)
    }

    fun createLineProcessor(
        context: ConverterFactory.ContentsContext
    ): Processor {
        return Processor(context) { l, t -> convert(l, t) }
    }

    fun getTagValue(root: Element, tag: String): String = root.getElementsByTagName(tag).item(0).getTextContent()

    fun getAttributeValue(root: Element, tag: String, attribute: String): String = (root.getElementsByTagName(tag).item(0) as Element).getAttribute(attribute)

    fun getAttributeValueFromElement(element: Element, attribute: String): String = element.getAttribute(attribute)

    fun getElementFromTagList(root: Element, tagList: List<String>): Element {
        // NOTE: This is assuming tags in each tree are unique
        // The tagList represents the branch leading to required element

        var currentElement: Element = root
        tagList.forEach { tag -> currentElement = currentElement.getElementsByTagName(tag).item(0) as Element }

        return currentElement
    }

    class Processor(val context: ConverterFactory.ContentsContext,
        private val conversion: Processor.(root: Element, timeReceived: Double) -> Sequence<TopicData>,
    )  {
        fun convertToRecord(
            root: Element,
            timeReceived: Double,
        ): Sequence<TopicData> = conversion(root, timeReceived)
    }


}


