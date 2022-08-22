package org.radarbase.connect.upload.converter.altoida_v2

import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.xml.XmlNodeProcessorFactory
import org.radarcns.connector.upload.altoida.*
import org.w3c.dom.Element

open class AltoidaTestEventXmlProcessor: XmlNodeProcessorFactory() {
    override val fileNameSuffix: String = "altoida_data.xml"

    override val nodeName: String = "events"

    override fun convertToSingleRecord(root: Element, timeReceived: Double, assessmentName: String?): TopicData? {
        val parent = root.parentNode.parentNode as Element

        return TopicData(
            topic = "connect_upload_altoida_test_event",
            value = AltoidaAssessmentEvent.newBuilder().apply {
                time = root.attributeToTime("ts")
                this.assessmentName = parent.assessmentName(assessmentName)
                assessmentTimestamp = parent.attributeToTime("start_ts")
                eventType = root.tagName
                objectName = root.objectName(assessmentName)
                location = root.getAttribute("location").toLocation()
            }.build()
        )
    }

    private fun Element.assessmentName(assessmentName: String?): String? {
        val objectName = getAttribute("object_name")

        return when {
            assessmentName != "ARTest" -> assessmentName
            objectName.isEmpty() -> "ARTest_$tagName"
            else -> "ARTest_${tagName}_$objectName"
        }
    }

    private fun Element.objectName(assessmentName: String?): String? = when (assessmentName) {
        "ContrastVisionTest" -> "circle_id_" + getAttribute("circle_id")
        else -> getAttribute("object_name")
    }

    private fun String.toLocation() : LocationValue {
        return when (this) {
            "top" -> LocationValue.TOP
            "right" -> LocationValue.RIGHT
            "left" -> LocationValue.LEFT
            "bottom" -> LocationValue.BOTTOM
            "topright" -> LocationValue.TOPRIGHT
            "topleft" -> LocationValue.TOPLEFT
            "bottomleft" -> LocationValue.BOTTOMLEFT
            "bottomright" -> LocationValue.BOTTOMRIGHT
            else -> LocationValue.UNKNOWN
        }
    }

}
