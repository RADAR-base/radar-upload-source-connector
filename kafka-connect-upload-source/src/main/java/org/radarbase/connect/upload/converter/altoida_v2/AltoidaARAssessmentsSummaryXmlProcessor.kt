package org.radarbase.connect.upload.converter.altoida_v2

import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.xml.XmlNodeProcessorFactory
import org.radarcns.connector.upload.altoida.*
import org.w3c.dom.Element

class AltoidaARAssessmentsSummaryXmlProcessor : XmlNodeProcessorFactory() {
    override val fileNameSuffix: String = "altoida_data.xml"

    override val nodeName: String = "multipart"

    override fun convertToSingleRecord(
        root: Element,
        timeReceived: Double,
        assessmentName: String?,
    ): TopicData = TopicData(
        topic = "connect_upload_altoida_ar_assessment",
        value = AltoidaAssessmentsSummary.newBuilder().apply {
            this.timeReceived = timeReceived
            this.assessmentName = root.tagName
            startTime = root.attributeToTime("start_ts")
            endTime = root.attributeToTime("end_ts")
            metadata = root.getAttribute("object_name")
        }.build()
    )
}
