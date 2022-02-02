package org.radarbase.connect.upload.converter.altoida_v2

import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.xml.XmlNodeProcessorFactory
import org.radarcns.connector.upload.altoida.*
import org.w3c.dom.Element

open class AltoidaScreenElementXmlProcessor: XmlNodeProcessorFactory() {
    override val fileNameSuffix: String = "altoida_data.xml"

    override val nodeName: String = "screen_elements"

    override fun convertToSingleRecord(root: Element, timeReceived: Double, assessmentName: String?): TopicData? {
        val parent = (if (assessmentName == "ContrastVisionTest") root.parentNode.parentNode.parentNode else root.parentNode.parentNode) as Element
        val assessmentTimestamp = getAttributeValueFromElement(parent, "start_ts")
        val newAssessmentName = getAssessmentName(parent, assessmentName)
        val elementName = root.tagName
        val xCenterOffset = getAttributeValue(root, "x_center_offset", "value").toDoubleOrNull()
        val yCenterOffset = getAttributeValue(root, "y_center_offset", "value").toDoubleOrNull()
        val id = getAttributeValueFromElement(root, "circle_id").toIntOrNull()
        val radius = getAttributeValue(root, "radius", "value").toDoubleOrNull()
        val alpha = getAttributeValue(root, "color", "alpha").toFloatOrNull()
        val red = getAttributeValue(root, "color", "r").toFloatOrNull()
        val green = getAttributeValue(root, "color", "g").toFloatOrNull()
        val blue = getAttributeValue(root, "color", "b").toFloatOrNull()
        val width = getAttributeValue(root, "width", "value").toDoubleOrNull()
        val height = getAttributeValue(root, "height", "value").toDoubleOrNull()


        return TopicData("connect_upload_altoida_screen_elements", AltoidaTestScreenElement(
                timeFieldParser.timeFromString(assessmentTimestamp),
                newAssessmentName,
                id,
                elementName,
                width,
                height,
                radius,
                xCenterOffset,
                yCenterOffset,
                alpha,
                red,
                green,
                blue
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

    override fun nodeConversions(
            root: Element,
            timeReceived: Double,
            assessmentName: String?
    ): Sequence<TopicData> {
        val elements = if (assessmentName == "ContrastVisionTest") root.firstChild.childNodes else root.childNodes
        return ((0 until elements.length)
                .asSequence()
                .filter { i -> elements.item(i).hasChildNodes() }
                .mapNotNull { i -> convertToSingleRecord(elements.item(i) as Element, timeReceived, assessmentName) })
    }

}
