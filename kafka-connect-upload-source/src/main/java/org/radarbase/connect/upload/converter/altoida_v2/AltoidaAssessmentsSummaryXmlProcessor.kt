package org.radarbase.connect.upload.converter.altoida_v2

import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.xml.XmlNodeProcessorFactory
import org.radarcns.connector.upload.altoida.*
import org.w3c.dom.Element

class AltoidaAssessmentsSummaryXmlProcessor : XmlNodeProcessorFactory() {
    override val fileNameSuffix: String = "altoida_data.xml"

    override val nodeName: String = "assessment"

    override fun convertToSingleRecord(root: Element, timeReceived: Double, assessmentName: String?): TopicData? {
        val timestamp = getAttributeValueFromElement(root, "start_ts")
        val endTime = getAttributeValueFromElement(root, "end_ts")
        val assessmentName = getAttributeValueFromElement(root, "xsi:type")

        return TopicData("connect_upload_altoida_assessment", AltoidaAssessmentsSummary(
                timeReceived,
                assessmentName,
                timeFieldParser.timeFromString(timestamp),
                timeFieldParser.timeFromString(endTime),
                null
        ))
    }

}
