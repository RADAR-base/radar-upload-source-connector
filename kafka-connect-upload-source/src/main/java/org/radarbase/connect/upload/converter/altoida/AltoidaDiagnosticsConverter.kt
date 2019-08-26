package org.radarbase.connect.upload.converter.altoida

import org.radarbase.connect.upload.converter.CsvRecordConverter
import org.radarbase.connect.upload.converter.TopicData
import org.radarcns.connector.upload.altoida.AltoidaDiagnostics

class AltoidaDiagnosticsConverter(override val sourceType: String = "altoida_diagnostics", val topic: String = "connect_upload_altoida_diagnostics")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "TAG", "PAYLOAD", "CONTRAST", "MOVEMENT", "ANGLE", "FEATURES") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val diagnostic = AltoidaDiagnostics(
                time,
                timeReceived,
                lineValues["TAG"],
                lineValues["PAYLOAD"]
        )

        return TopicData(false, topic, diagnostic)
    }
}
