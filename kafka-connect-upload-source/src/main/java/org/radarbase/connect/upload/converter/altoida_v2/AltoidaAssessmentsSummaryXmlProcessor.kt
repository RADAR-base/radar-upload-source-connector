package org.radarbase.connect.upload.converter.altoida_v2

import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.xml.XmlNodeProcessorFactory
import org.radarcns.connector.upload.altoida.*
import org.w3c.dom.Element

class AltoidaAssessmentsSummaryXmlProcessor : XmlNodeProcessorFactory() {
    override val fileNameSuffix: String = "altoida_data.xml"

    override val nodeName: String = "assessment"

    override fun convertToSingleRecord(
        root: Element,
        timeReceived: Double,
        assessmentName: String?,
    ): TopicData {
        val timestamp = getAttributeValueFromElement(root, "start_ts")
        val endTime = getAttributeValueFromElement(root, "end_ts")
        val innerAssessmentName = requireNotNull(
            getAttributeValueFromElement(root, "xsi:type")
                .takeIf { it.isNotEmpty() }
                ?: assessmentName
        )

        return TopicData(
            topic = "connect_upload_altoida_assessment",
            value = AltoidaAssessmentsSummary(
                timeReceived,
                innerAssessmentName,
                timeFieldParser.timeFromString(timestamp),
                timeFieldParser.timeFromString(endTime),
                null,
            ),
        )
    }

}
