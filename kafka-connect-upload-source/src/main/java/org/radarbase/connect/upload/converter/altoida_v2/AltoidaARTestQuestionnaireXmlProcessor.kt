package org.radarbase.connect.upload.converter.altoida_v2

import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.xml.XmlNodeProcessorFactory
import org.radarcns.connector.upload.altoida.*
import org.w3c.dom.Element

class AltoidaARTestQuestionnaireXmlProcessor : XmlNodeProcessorFactory() {
    override val fileNameSuffix: String = "altoida_data.xml"

    override val nodeName: String = "questionnaire"

    override fun convertToSingleRecord(root: Element, timeReceived: Double, assessmentName: String?): TopicData? {
        val timestamp = getAttributeValueFromElement(root, "start_ts")
        val firstObjectPlaced = getAttributeValue(root, "first_object_placed", "selected_object")
        val firstObjectPlacedCorrect = getAttributeValue(root, "first_object_placed", "correct_object")
        val firstObjectSearched = getAttributeValue(root, "first_object_searched", "selected_object")
        val firstObjectSearchedCorrect = getAttributeValue(root, "first_object_searched", "correct_object")

        return TopicData("connect_upload_altoida_ar_test_questionnaire", AltoidaARTestQuestionnaire(
                timeFieldParser.timeFromString(timestamp),
                timeReceived,
                firstObjectPlaced,
                firstObjectPlacedCorrect,
                firstObjectSearched,
                firstObjectSearchedCorrect
        ))
    }

}
