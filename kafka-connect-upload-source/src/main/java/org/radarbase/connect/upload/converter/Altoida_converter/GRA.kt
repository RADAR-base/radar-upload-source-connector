package org.radarbase.connect.upload.converter

import Altoida.avroSchemas.PhoneGravity

class GravityConverter(override val sourceType: String = "phone-gravity", val topic: String = "altoida-phone-gravity")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "X", "Y", "Z") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val gravity = PhoneGravity(
                time,
                timeReceived,
                lineValues["X"]?.toFloat(),
                lineValues["Y"]?.toFloat(),
                lineValues["Z"]?.toFloat()
        )

        return TopicData(false, topic, gravity)
    }
}