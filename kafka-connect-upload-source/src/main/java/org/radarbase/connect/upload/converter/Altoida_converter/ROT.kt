package org.radarbase.connect.upload.converter

import Altoida.avroSchemas.PhoneRotation

class RotationConverter(override val sourceType: String = "phone-rotation", val topic: String = "altoida-phone-rotation ")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "X", "Y", "Z") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val rotation = PhoneRotation(
                time,
                timeReceived,
                lineValues["X"]?.toFloat(),
                lineValues["Y"]?.toFloat(),
                lineValues["Z"]?.toFloat()
        )

        return TopicData(false, topic, rotation)
    }
}