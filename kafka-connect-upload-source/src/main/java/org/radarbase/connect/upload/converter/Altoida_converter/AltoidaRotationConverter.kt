package org.radarbase.connect.upload.converter

import RADAR-Schemas.commons.connector.altoida..AltoidaRotation

class AltoidaRotationConverter(override val sourceType: String = "rotation", val topic: String = "altoida-rotation ")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "X", "Y", "Z") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val rotation = AltoidaRotation(
                time,
                timeReceived,
                lineValues["X"]?.toFloat(),
                lineValues["Y"]?.toFloat(),
                lineValues["Z"]?.toFloat()
        )

        return TopicData(false, topic, rotation)
    }
}