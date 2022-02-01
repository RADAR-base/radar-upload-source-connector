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
        val assessmentName = getAttributeValueFromElement(parent, "xsi:type")
        val elementName = root.tagName
        val id = getAttributeValueFromElement(root, "circle_id")
        val width = getAttributeValue(root, "width", "value")
        val height = getAttributeValue(root, "height", "value")
        val radius = getAttributeValue(root, "radius", "value")
        val alpha = getAttributeValue(root, "color", "alpha")
        val red = getAttributeValue(root, "color", "r")
        val green = getAttributeValue(root, "color", "g")
        val blue = getAttributeValue(root, "color", "b")
        val xCenterOffset = getAttributeValue(root, "x_center_offset", "value")
        val yCenterOffset = getAttributeValue(root, "x_center_offset", "value")

        return TopicData("connect_upload_altoida_screen_elements", AltoidaTestScreenElement(
                timeFieldParser.timeFromString(assessmentTimestamp),
                assessmentName,
                id.toInt(),
                elementName,
                width,
                height,
                radius.toDouble(),
                xCenterOffset.toDouble(),
                yCenterOffset.toDouble(),
                alpha.toFloat(),
                red.toFloat(),
                green.toFloat(),
                blue.toFloat()
        ))
    }

    override fun nodeConversions(
            root: Element,
            timeReceived: Double
    ): Sequence<TopicData> {
        val elements = if (root.firstChild.nodeName == "contrast_circles") root.firstChild.childNodes else root.childNodes
        return ((0 until elements.length)
                .asSequence()
                .filter { i -> elements.item(i).hasAttributes() }
                .mapNotNull { i -> convertToSingleRecord(elements.item(i) as Element, timeReceived) })
    }

}
