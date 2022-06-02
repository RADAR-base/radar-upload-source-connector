package org.radarbase.connect.upload.converter.altoida_v2

import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.xml.XmlNodeProcessorFactory
import org.radarcns.connector.upload.altoida.*
import org.w3c.dom.Element

class AltoidaARTestQuestionnaireXmlProcessor : XmlNodeProcessorFactory() {
    override val fileNameSuffix: String = "altoida_data.xml"

    override val nodeName: String = "questionnaire"

    override fun convertToSingleRecord(
        root: Element,
        timeReceived: Double,
        assessmentName: String?,
    ): TopicData {
        val placedElement = root.childOrNull("first_object_placed")
        val searchedElement = root.childOrNull("first_object_searched")

        return TopicData(
            topic = "connect_upload_altoida_ar_test_questionnaire",
            value = AltoidaARTestQuestionnaire.newBuilder().apply {
                time = root.attributeToTime("start_ts")
                this.timeReceived = timeReceived
                firstObjectPlaced = placedElement.attribute("selected_object")
                firstObjectPlacedCorrect = placedElement.attribute("correct_object")
                firstObjectSearched = searchedElement.attribute("selected_object")
                firstObjectSearchedCorrect = searchedElement.attribute("correct_object")
            }.build(),
        )
    }

}
