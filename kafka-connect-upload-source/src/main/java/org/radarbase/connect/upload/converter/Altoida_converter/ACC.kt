package org.radarbase.connect.upload.converter

import Altoida.avroSchemas.PhoneAcceleration

class AccelerometerConverter(override val sourceType: String = "phone-acceleration", val topic: String = "altoida-phone-acceleration")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "X", "Y", "Z") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val acceleration = PhoneAcceleration(
                time,
                timeReceived,
                lineValues["X"]?.toFloat(),
                lineValues["Y"]?.toFloat(),
                lineValues["Z"]?.toFloat()
        )

        return TopicData(false, topic, acceleration)
    }
}