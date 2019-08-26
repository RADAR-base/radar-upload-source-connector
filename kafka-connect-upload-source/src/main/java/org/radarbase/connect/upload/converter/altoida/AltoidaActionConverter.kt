package org.radarbase.connect.upload.converter.altoida

import org.radarbase.connect.upload.converter.CsvRecordConverter
import org.radarbase.connect.upload.converter.TopicData
import org.radarcns.connector.upload.altoida.AltoidaAction

class AltoidaActionConverter(override val sourceType: String = "altoida_action", val topic: String = "connect_upload_altoida_action")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "TAG", "PAYLOAD") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val log = AltoidaAction(
                time,
                timeReceived,
                lineValues["TAG"],
                lineValues["PAYLOAD"]
        )

        return TopicData(false, topic, log)
    }
}

