package org.radarbase.connect.upload.converter.altoida

import org.radarbase.connect.upload.converter.CsvRecordConverter
import org.radarbase.connect.upload.converter.TopicData
import org.radarcns.connector.altoida.UploadAltoidaMagnetometer

class MagnetometerCsvRecordConverter(override val sourceType: String = "magnetometer", val topic: String = "connect_upload_altoida_magnetometer")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "X", "Y", "Z", "ACCURACY") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val magnetometer = UploadAltoidaMagnetometer(
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
