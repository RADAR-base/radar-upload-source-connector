package org.radarbase.connect.upload.converter.altoidav2

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

    override fun convertToSingleRecord(
        root: Element,
        timeReceived: Double,
        assessmentName: String?,
    ): TopicData {
        val deviceElement = root.childOrNull("device")
        val osElement = root.childOrNull("os")
        val displayElement = root.child("display")
        val displaySizeElement = displayElement.child("size")

        return TopicData(
            topic = "connect_upload_altoida_xml_metadata",
            value = AltoidaXmlMetadata.newBuilder().apply {
                time = root.child("datetime").attributeToTime("utc")
                this.timeReceived = timeReceived
                age = root.childValue("age").toInt()
                yearsOfEducation = root.childValue("years_of_education").toIntOrNull()
                gender = root.childValue("gender").toGender()
                dominantHand = root.childValue("dominant_hand").toDominantHand()
                applicationVersion = root.childOrNull("application").attribute("version")
                deviceType = deviceElement.attribute("type").toDeviceType()
                deviceDescription = deviceElement.attribute("description")
                osType = osElement.attribute("type").toOsType()
                osVersion = osElement.attribute("version")
                displayPpi = displayElement.attribute("ppi").toDouble()
                displayWidthPixels = displaySizeElement
                    .child("width", "pixels")
                    .getAttribute("value")
                    .toDouble()
                displayHeightPixels = displaySizeElement
                    .child("height", "pixels")
                    .getAttribute("value")
                    .toDouble()
                displayWidthCm = displaySizeElement
                    .child("width", "centimeters")
                    .getAttribute("value")
                    .toDouble()
                displayHeightCm = displaySizeElement
                    .child("height", "centimeters")
                    .getAttribute("value")
                    .toDouble()
            }.build(),
        )
    }

    override fun nodeConversions(
        root: Element,
        timeReceived: Double,
        assessmentName: String?,
    ) = sequenceOf(
        convertToSingleRecord(root, timeReceived, assessmentName),
    )

    private fun String.toGender(): GenderType = when (this) {
        "male" -> GenderType.MALE
        "female" -> GenderType.FEMALE
        "other" -> GenderType.OTHER
        else -> GenderType.UNKNOWN
    }

    private fun String.toDominantHand(): DominantHandType = when (this) {
        "left" -> DominantHandType.LEFT
        "right" -> DominantHandType.RIGHT
        "other" -> DominantHandType.OTHER
        else -> DominantHandType.UNKNOWN
    }

    private fun String.toDeviceType(): DeviceType = when (this) {
        "phone" -> DeviceType.PHONE
        "tablet" -> DeviceType.TABLET
        "other" -> DeviceType.OTHER
        else -> DeviceType.UNKNOWN
    }

    private fun String.toOsType(): OSType = when (this) {
        "iOS" -> OSType.IOS
        "android" -> OSType.ANDROID
        "other" -> OSType.OTHER
        else -> OSType.UNKNOWN
    }

    companion object {
        private const val defaultTimeFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'"
        val defaultTimeFormatter = defaultTimeFormat.formatTimeFieldParser()
    }
}
