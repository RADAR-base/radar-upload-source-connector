package org.radarbase.connect.upload.converter

import org.radarcns.connector.altoida.AltoidaGravity

class AltoidaGravityConverter(override val sourceType: String = "gravity", val topic: String = "connect_upload_altoida_gravity")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "X", "Y", "Z") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val gravity = AltoidaGravity(
                time,
                timeReceived,
                lineValues["X"]?.toFloat(),
                lineValues["Y"]?.toFloat(),
                lineValues["Z"]?.toFloat()
        )

        return TopicData(false, topic, gravity)
    }
}
