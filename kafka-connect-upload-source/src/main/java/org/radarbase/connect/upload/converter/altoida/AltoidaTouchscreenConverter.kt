package org.radarbase.connect.upload.converter.altoida

import org.radarbase.connect.upload.converter.CsvRecordConverter
import org.radarbase.connect.upload.converter.TopicData
import org.radarcns.connector.upload.altoida.AltoidaTouchscreen

class AltoidaTouchscreenConverter(override val sourceType: String = "altoida_touchscreen", val topic: String = "connect_upload_altoida_touchscreen")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "X", "Y") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val normalVector = AltoidaTouchscreen(
                time,
                timeReceived,
                lineValues["X"]?.toDouble(),
                lineValues["Y"]?.toDouble()
        )

        return TopicData(false, topic, normalVector)
    }
}
