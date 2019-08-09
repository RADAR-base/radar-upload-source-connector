package org.radarbase.connect.upload.converter

import org.radarcns.connector.altoida.AltoidaAcceleration

class AltoidaAccelerometerConverter(override val sourceType: String = "acceleration", val topic: String = "upload_altoida_acceleration")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "X", "Y", "Z") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val acceleration = AltoidaAcceleration(
                time,
                timeReceived,
                lineValues["X"]?.toFloat(),
                lineValues["Y"]?.toFloat(),
                lineValues["Z"]?.toFloat()
        )

        return TopicData(false, topic, acceleration)
    }
}
