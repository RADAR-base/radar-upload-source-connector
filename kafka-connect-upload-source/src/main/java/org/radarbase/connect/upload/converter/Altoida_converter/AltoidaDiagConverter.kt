package org.radarbase.connect.upload.converter

import RADAR-Schemas.commons.connector.altoida.AltoidaDiag

class AltoidaDiagConverter(override val sourceType: String = "diag", val topic: String = "altoida-diag")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "TAG", "PAYLOAD", "CONTRAST", "MOVEMENT", "ANGLE", "FEATURES") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val diag = AltoidaDiag(
                time,
                timeReceived,
                lineValues["TAG"]?.toString(),
                lineValues["PAYLOAD"]?.toString(),
                lineValues["CONTRAST"]?.toFloat(),
                lineValues["MOVEMENT"]?.toFloat(),
                lineValues["ANGLE"]?.toFloat(),
                lineValues["FEATURES"]?.toFloat()


        )

        return TopicData(false, topic, diag)
    }
}