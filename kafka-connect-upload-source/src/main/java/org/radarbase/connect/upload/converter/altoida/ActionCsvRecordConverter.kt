package org.radarbase.connect.upload.converter.altoida

import org.radarbase.connect.upload.converter.CsvRecordConverter
import org.radarbase.connect.upload.converter.TopicData
import org.radarcns.connector.altoida.UploadAltoidaAction

class ActionCsvRecordConverter(override val sourceType: String = "log", val topic: String = "connect_upload_altoida_action")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "TAG", "PAYLOAD") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val log = UploadAltoidaAction(
                time,
                timeReceived,
                lineValues["TAG"]?.toString(),
                lineValues["PAYLOAD"]?.toString()

        )

        return TopicData(false, topic, log)
    }
}

