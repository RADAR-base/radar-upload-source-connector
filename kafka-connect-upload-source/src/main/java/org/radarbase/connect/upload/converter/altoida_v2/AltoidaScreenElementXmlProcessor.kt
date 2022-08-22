package org.radarbase.connect.upload.converter.altoida_v2

import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.xml.XmlNodeProcessorFactory
import org.radarcns.connector.upload.altoida.*
import org.w3c.dom.Element

open class AltoidaScreenElementXmlProcessor: XmlNodeProcessorFactory() {
    override val fileNameSuffix: String = "altoida_data.xml"

    override val nodeName: String = "screen_elements"

    override fun convertToSingleRecord(
        root: Element,
        timeReceived: Double,
        assessmentName: String?,
    ): TopicData {
        val parent = (if (assessmentName == "ContrastVisionTest") root.parentNode.parentNode.parentNode else root.parentNode.parentNode) as Element

        val colorElement = root.childOrNull("color")

        return TopicData(
            topic = "connect_upload_altoida_screen_elements",
            value = AltoidaTestScreenElement.newBuilder().apply {
                time = parent.attributeToTime("start_ts")
                this.assessmentName = parent.assessmentName(assessmentName)
                id = root.getAttribute("circle_id").toIntOrNull()
                name = root.tagName
                width = root.childOrNull( "width").attribute("value").toDoubleOrNull()
                height = root.childOrNull( "height").attribute("value").toDoubleOrNull()
                radius = root.childOrNull("radius").attribute("value").toDoubleOrNull()
                xCenterOffset = root.childOrNull( "x_center_offset").attribute("value").toDoubleOrNull()
                yCenterOffset = root.childOrNull( "y_center_offset").attribute("value").toDoubleOrNull()
                colorAlpha = colorElement.attribute("alpha").toFloatOrNull()
                colorRed = colorElement.attribute("r").toFloatOrNull()
                colorGreen = colorElement.attribute("g").toFloatOrNull()
                colorBlue = colorElement.attribute("b").toFloatOrNull()
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

    override fun nodeConversions(
        root: Element,
        timeReceived: Double,
        assessmentName: String?,
    ): Sequence<TopicData> {
        val elements = if (assessmentName == "ContrastVisionTest") root.firstChild.childNodes else root.childNodes
        return elements.asSequence()
            .filter { it.hasChildNodes() && it is Element }
            .map { convertToSingleRecord(it as Element, timeReceived, assessmentName) }
    }
}
