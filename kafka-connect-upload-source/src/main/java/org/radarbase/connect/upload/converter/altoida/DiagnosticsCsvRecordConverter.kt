package org.radarbase.connect.upload.converter.altoida

import org.radarbase.connect.upload.converter.CsvRecordConverter
import org.radarbase.connect.upload.converter.TopicData
import org.radarcns.connector.altoida.UploadAltoidaDiagnostics

class DiagnosticsCsvRecordConverter(override val sourceType: String = "diag", val topic: String = "connect_upload_altoida_diagnostics")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "TAG", "PAYLOAD", "CONTRAST", "MOVEMENT", "ANGLE", "FEATURES") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val diagnostics = UploadAltoidaDiagnostics(
                time,
                timeReceived,
                lineValues["TAG"]?.toString(),
                lineValues["PAYLOAD"]?.toString(),
                lineValues["CONTRAST"]?.toFloat(),
                lineValues["MOVEMENT"]?.toFloat(),
                lineValues["ANGLE"]?.toFloat(),
                lineValues["FEATURES"]?.toFloat()


        )

        return TopicData(false, topic, diagnostics)
    }
}
