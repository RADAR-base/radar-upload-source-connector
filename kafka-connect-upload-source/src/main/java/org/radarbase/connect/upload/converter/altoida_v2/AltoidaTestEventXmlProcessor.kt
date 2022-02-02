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
        val assessmentTimestamp = getAttributeValueFromElement(parent, "start_ts")
        val newAssessmentName = getAssessmentName(parent, assessmentName)
        val eventType = root.tagName
        val time = getAttributeValueFromElement(root, "ts")
        val objectName = getObjectName(root, assessmentName)
        val location = getAttributeValueFromElement(root, "location").toLocation()

    return TopicData("connect_upload_altoida_test_event", AltoidaAssessmentEvent(
            timeFieldParser.timeFromString(time),
            newAssessmentName,
            timeFieldParser.timeFromString(assessmentTimestamp),
            eventType,
            objectName,
            location
        ))
    }

    fun getAssessmentName(root: Element, assessmentName: String?): String? {
        val objectName = root.getAttribute("object_name")
        return when (assessmentName) {
            "ARTest" -> {
                val arName = "ARTest_" + root.tagName
                if (objectName.length > 0) arName + "_" + objectName else arName
            }
            else -> assessmentName
        }
    }

    fun getObjectName(root: Element, assessmentName: String?): String? {
        return when (assessmentName) {
            "ContrastVisionTest" -> "circle_id_" + getAttributeValueFromElement(root, "circle_id")
            else -> getAttributeValueFromElement(root, "object_name")
        }
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
