package org.radarbase.connect.upload.converter.altoida

import org.radarbase.connect.upload.converter.CsvRecordConverter
import org.radarbase.connect.upload.converter.TopicData
import org.radarcns.connector.altoida.UploadAltoidaTouchscreen

class TouchscreenCsvRecordConverter(override val sourceType: String = "touchscreen", val topic: String = "connect_upload_altoida_touchscreen")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "X", "Y") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val normalVector = UploadAltoidaTouchscreen(
                time,
                timeReceived,
                lineValues["X"]?.toFloat(),
                lineValues["Y"]?.toFloat()

        )

        return TopicData(false, topic, normalVector)
    }
}
