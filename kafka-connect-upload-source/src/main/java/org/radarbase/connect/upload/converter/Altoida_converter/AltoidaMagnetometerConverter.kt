package org.radarbase.connect.upload.converter

import org.radarcns.connector.altoida.AltoidaMagnetometer

class AltoidaMagnetometerConverter(override val sourceType: String = "magnetometer", val topic: String = "upload_altoida_magnetometer")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "X", "Y", "Z", "ACCURACY") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val magnetometer = AltoidaMagnetometer(
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
