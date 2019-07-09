package org.radarbase.connect.upload.converter

import Altoida.avroSchemas.PhoneMagnetometer

class MagnetometerConverter(override val sourceType: String = "phone-magnetometer", val topic: String = "altoida-phone-magnetometer")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "X", "Y", "Z", "ACCURACY") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val magnetometer = PhoneMagnetometer(
                time,
                timeReceived,
                lineValues["X"]?.toFloat(),
                lineValues["Y"]?.toFloat(),
                lineValues["Z"]?.toFloat(),
                lineValues["ACCURACY"]?.toString()
        )

        return TopicData(false, topic, magnetometer)
    }
}