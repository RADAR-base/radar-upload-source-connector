package org.radarbase.connect.upload.converter.altoida_v2

import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.xml.XmlNodeProcessorFactory
import org.radarcns.connector.upload.altoida.*
import org.w3c.dom.Element

open class AltoidaScreenElementXmlProcessor: XmlNodeProcessorFactory() {
    override val fileNameSuffix: String = "altoida_data.xml"

    override val nodeName: String = "screen_elements"

    override fun convertToSingleRecord(root: Element, timeReceived: Double, assessmentName: String?): TopicData? {
        val parent = root.parentNode.parentNode.parentNode as Element
        val assessmentTimestamp = getAttributeValueFromElement(parent, "start_ts")
        val newAssessmentName = getAssessmentName(parent, assessmentName)
        val elementName = root.tagName
        val xCenterOffset = getAttributeValue(root, "x_center_offset", "value").toDouble()
        val yCenterOffset = getAttributeValue(root, "y_center_offset", "value").toDouble()

        var id: Int? = null; var width: Double? = null; var height: Double? = null; var radius: Double? = null
        var alpha: Float? = null; var red: Float? = null; var green: Float? = null; var blue: Float? = null

        when (assessmentName) {
            "ContrastVisionTest" -> {
                id = getAttributeValueFromElement(root, "circle_id").toInt()
                radius = getAttributeValue(root, "radius", "value").toDouble()
                alpha = getAttributeValue(root, "color", "alpha").toFloat()
                red = getAttributeValue(root, "color", "r").toFloat()
                green = getAttributeValue(root, "color", "g").toFloat()
                blue = getAttributeValue(root, "color", "b").toFloat()
            }
            else -> {
                width = getAttributeValue(root, "width", "value").toDouble()
                height = getAttributeValue(root, "height", "value").toDouble()
            }
        }

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
        return when (assessmentName) {
            "ARTest" -> "ARTest_" + root.tagName
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
                .filter { i -> elements.item(i).hasAttributes() }
                .mapNotNull { i -> convertToSingleRecord(elements.item(i) as Element, timeReceived, assessmentName) })
    }

}
