package org.radarbase.connect.upload.converter.altoida_v2

import org.radarbase.connect.upload.converter.TimeFieldParser.DateFormatParser.Companion.formatTimeFieldParser
import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.xml.XmlProcessor
import org.radarcns.connector.upload.altoida.AltoidaXmlMetadata
import org.radarcns.connector.upload.altoida.GenderType
import org.radarcns.connector.upload.altoida.DominantHandType
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.time.Instant

class AltoidaMetadataXmlProcessor : XmlProcessor() {
    override val fileNameSuffix: String = "altoida_data.xml"

    override val timeFieldParser = defaultTimeFormatter

    override fun convertToSingleRecord(root: Element, timeReceived: Double): TopicData? {
        val timestamp = getAttributeValue(root, "datetime", "utc")
        val age = getTagValue(root, "age")
        val yearsOfEducation = getTagValue(root, "years_of_education")
        val gender = getTagValue(root, "gender").toGender()
        val dominantHand = getTagValue(root, "dominant_hand").toDominantHand()

        val applicationVersion = getAttributeValue(root, "application", "version")
        val deviceType = getAttributeValue(root, "device", "type")
        val deviceDescription = getAttributeValue(root, "device", "description")
        val osType = getAttributeValue(root, "os", "type")
        val osVersion = getAttributeValue(root, "os", "version")
        val displayPpi = getAttributeValue(root, "display", "ppi")

        val displayWidthPixelsList = listOf("display", "size", "width", "pixels")
        val displayWidthPixels =  getAttributeValueFromElement(getElementFromTagList(root, displayWidthPixelsList), "value")

        val displayHeightPixelsList = listOf("display", "size", "height", "pixels")
        val displayHeightPixels = getAttributeValueFromElement(getElementFromTagList(root, displayHeightPixelsList), "value")

        val displayWidthCmList = listOf("display", "size", "height", "centimeters")
        val displayWidthCm = getAttributeValueFromElement(getElementFromTagList(root, displayWidthCmList), "value")

        val displayHeightCmList = listOf("display", "size", "height", "centimeters")
        val displayHeightCm = getAttributeValueFromElement(getElementFromTagList(root, displayHeightCmList), "value")

        return TopicData("connect_upload_altoida_xml_metadata", AltoidaXmlMetadata(
                timeFieldParser.timeFromString(timestamp),
                timeReceived,
                age,
                yearsOfEducation,
                gender,
                dominantHand,
                applicationVersion,
                deviceType,
                deviceDescription,
                osType,
                osVersion,
                displayPpi,
                displayWidthPixels,
                displayHeightPixels,
                displayWidthCm,
                displayHeightCm)
        )
    }

    private fun String.toGender() : GenderType {
        return when (this) {
            "male" -> GenderType.MALE
            "female" -> GenderType.FEMALE
            "other" -> GenderType.OTHER
            else -> GenderType.UNKNOWN
        }
    }

    private fun String.toDominantHand() : DominantHandType {
        return when (this) {
            "left" -> DominantHandType.LEFT
            "right" -> DominantHandType.RIGHT
            "other" -> DominantHandType.OTHER
            else -> DominantHandType.UNKNOWN
        }
    }


    companion object {
        private const val defaultTimeFormat = "yyyy-MM-dd HH:mm:ss"
        val defaultTimeFormatter = defaultTimeFormat.formatTimeFieldParser()
    }
}
