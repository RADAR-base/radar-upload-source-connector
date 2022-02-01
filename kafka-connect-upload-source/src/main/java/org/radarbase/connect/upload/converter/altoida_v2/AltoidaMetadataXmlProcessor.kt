package org.radarbase.connect.upload.converter.altoida_v2

import org.radarbase.connect.upload.converter.TimeFieldParser.DateFormatParser.Companion.formatTimeFieldParser
import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.xml.XmlNodeProcessorFactory
import org.radarcns.connector.upload.altoida.AltoidaXmlMetadata
import org.radarcns.connector.upload.altoida.DeviceType
import org.radarcns.connector.upload.altoida.DominantHandType
import org.radarcns.connector.upload.altoida.GenderType
import org.radarcns.connector.upload.altoida.OSType
import org.w3c.dom.Element

class AltoidaMetadataXmlProcessor : XmlNodeProcessorFactory() {
    override val fileNameSuffix: String = "altoida_data.xml"

    override val timeFieldParser = defaultTimeFormatter

    override val nodeName: String = "metadata"

    override fun convertToSingleRecord(root: Element, timeReceived: Double, assessmentName: String?): TopicData? {
        val timestamp = getAttributeValue(root, "datetime", "utc")
        val age = getTagValue(root, "age").toInt()
        val yearsOfEducation = getTagValue(root, "years_of_education").toInt()
        val gender = getTagValue(root, "gender").toGender()
        val dominantHand = getTagValue(root, "dominant_hand").toDominantHand()

        val applicationVersion = getAttributeValue(root, "application", "version")
        val deviceType = getAttributeValue(root, "device", "type")
        val deviceDescription = getAttributeValue(root, "device", "description")
        val osType = getAttributeValue(root, "os", "type")
        val osVersion = getAttributeValue(root, "os", "version")
        val displayPpi = getAttributeValue(root, "display", "ppi")

        val displayWidthPixelsList = listOf("display", "size", "width", "pixels")
        val displayWidthPixels =  getAttributeValueFromElement(getSingleElementFromTagList(root, displayWidthPixelsList), "value")

        val displayHeightPixelsList = listOf("display", "size", "height", "pixels")
        val displayHeightPixels = getAttributeValueFromElement(getSingleElementFromTagList(root, displayHeightPixelsList), "value")

        val displayWidthCmList = listOf("display", "size", "height", "centimeters")
        val displayWidthCm = getAttributeValueFromElement(getSingleElementFromTagList(root, displayWidthCmList), "value")

        val displayHeightCmList = listOf("display", "size", "height", "centimeters")
        val displayHeightCm = getAttributeValueFromElement(getSingleElementFromTagList(root, displayHeightCmList), "value")

        return TopicData("connect_upload_altoida_xml_metadata", AltoidaXmlMetadata(
                timeFieldParser.timeFromString(timestamp),
                timeReceived,
                age,
                yearsOfEducation,
                gender,
                dominantHand,
                applicationVersion,
                deviceType.toDeviceType(),
                deviceDescription,
                osType.toOsType(),
                osVersion,
                displayPpi.toDouble(),
                displayWidthPixels.toDouble(),
                displayHeightPixels.toDouble(),
                displayWidthCm.toDouble(),
                displayHeightCm.toDouble())
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

    private fun String.toDeviceType() : DeviceType {
        return when (this) {
            "phone" -> DeviceType.PHONE
            "tablet" -> DeviceType.TABLET
            "other" -> DeviceType.OTHER
            else -> DeviceType.UNKNOWN
        }
    }

    private fun String.toOsType() : OSType {
        return when (this) {
            "iOS" -> OSType.IOS
            "android" -> OSType.ANDROID
            "other" -> OSType.OTHER
            else -> OSType.UNKNOWN
        }
    }

    companion object {
        private const val defaultTimeFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'"
        val defaultTimeFormatter = defaultTimeFormat.formatTimeFieldParser()
    }
}
