package org.radarbase.connect.upload.converter

import org.radarcns.connector.altoida.AltoidaPath

class AltoidaPathConverter(override val sourceType: String = "path", val topic: String = "upload_altoida_path")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "X", "Y", "Z") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val path = AltoidaPath(
                time,
                timeReceived,
                lineValues["X"]?.toFloat(),
                lineValues["Y"]?.toFloat(),
                lineValues["Z"]?.toFloat()

        )

        return TopicData(false, topic, path)
    }
}
