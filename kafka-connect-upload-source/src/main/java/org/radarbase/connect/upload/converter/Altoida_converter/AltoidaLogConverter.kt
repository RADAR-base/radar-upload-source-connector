package org.radarbase.connect.upload.converter

import RADAR-Schemas.commons.connector.altoida.AltoidaLog

class AltoidaLogConverter(override val sourceType: String = "log", val topic: String = "altoida-log")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "TAG", "PAYLOAD") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val log = AltoidaLog(
                time,
                timeReceived,
                lineValues["TAG"]?.toString(),
                lineValues["PAYLOAD"]?.toString()

        )

        return TopicData(false, topic, log)
    }
}

