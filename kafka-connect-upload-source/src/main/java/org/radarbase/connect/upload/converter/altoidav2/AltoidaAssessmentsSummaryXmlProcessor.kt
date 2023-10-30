package org.radarbase.connect.upload.converter.altoidav2

import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.xml.XmlNodeProcessorFactory
import org.radarcns.connector.upload.altoida.AltoidaAssessmentsSummary
import org.w3c.dom.Element

class AltoidaAssessmentsSummaryXmlProcessor : XmlNodeProcessorFactory() {
    override val fileNameSuffix: String = "altoida_data.xml"

    override val nodeName: String = "assessment"

    override fun convertToSingleRecord(
        root: Element,
        timeReceived: Double,
        assessmentName: String?,
    ): TopicData {
        return TopicData(
            topic = "connect_upload_altoida_assessment",
            value = AltoidaAssessmentsSummary.newBuilder().apply {
                this.timeReceived = timeReceived
                this.assessmentName = root.attribute("xsi:type", default = assessmentName ?: "")
                startTime = root.attributeToTime("start_ts")
                endTime = root.attributeToTime("end_ts")
                metadata = null
            }.build(),
        )
    }
}
