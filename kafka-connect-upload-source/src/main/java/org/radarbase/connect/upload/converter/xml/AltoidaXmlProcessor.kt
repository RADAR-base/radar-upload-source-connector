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

import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.TopicData
import org.w3c.dom.Element

/**
 * Altoida class to process XML files with multiple possible per-node/tag processors.
 */
open class AltoidaXmlProcessor(
        private val record: RecordDTO? = null,
        private val processorFactories: List<XmlNodeProcessorFactory>? = emptyList(),
): XmlProcessor(record, processorFactories) {

    /**
     * Recursively traverses the xml nodes and converts each node if processor (that matches node name) exists.
     * In addition to similar base class method, this extracts the assessment name that corresponds to the tag.
     */
    override fun processXml(root: Element, list: MutableList<TopicData>, context: ConverterFactory.ContentsContext, contentProcessorsFactories: List<XmlNodeProcessorFactory.XmlNodeProcessor>, assessmentName: String) {
        val children = root.childNodes
        for (i in 0 until children.length) {
            var n = children.item(i)
            if (n.hasChildNodes()) {
                n = n as Element
                val nodeName = n.nodeName
                var updatedAssessmentName = assessmentName
                if (nodeName == "part") {
                    val name = n.getAttribute("xsi:type")
                    updatedAssessmentName = if (name.isNullOrEmpty()) updatedAssessmentName else name
                }
                val processor = contentProcessorsFactories.firstOrNull { it.matches(nodeName) }
                if (processor != null) {
                    val topicData = processor.convertToRecord(n, context.timeReceived)
                    list.addAll(topicData)
                }
                processXml(n, list, context, contentProcessorsFactories, updatedAssessmentName)
            }
        }
    }

}


