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
        record: RecordDTO? = null,
        processorFactories: List<XmlNodeProcessorFactory>,
): XmlProcessor(record, processorFactories) {

    /**
     * Recursively traverses the xml nodes and converts each node if processor (that matches node name) exists.
     * In addition to similar base class method, this extracts the assessment name that corresponds to the tag.
     */
    override fun processXml(
        root: Element,
        context: ConverterFactory.ContentsContext,
        contentProcessorsFactories: List<XmlNodeProcessorFactory.XmlNodeProcessor>,
        metadata: String,
        produce: (TopicData) -> Unit,
    ) {
        val children = root.childNodes
        for (i in 0 until children.length) {
            val n = children.item(i)
            if (n.hasChildNodes()) {
                n as Element
                val nodeName = n.nodeName
                var updatedAssessmentName = metadata
                if (nodeName == "part") {
                    val name = n.getAttribute("xsi:type")
                    if (!name.isNullOrEmpty()) {
                        updatedAssessmentName = name
                    }
                }
                contentProcessorsFactories
                    .firstOrNull { it.matches(nodeName) }
                    ?.convertToRecord(
                        node = n,
                        timeReceived = context.timeReceived,
                        assessmentName = updatedAssessmentName
                    )
                    ?.forEach(produce)

                processXml(n, context, contentProcessorsFactories, updatedAssessmentName, produce)
            }
        }
    }

}


