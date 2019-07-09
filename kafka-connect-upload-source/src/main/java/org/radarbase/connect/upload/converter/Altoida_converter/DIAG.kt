package org.radarbase.connect.upload.converter

import Altoida.avroSchemas.PhoneDiag

class DiagConverter(override val sourceType: String = "phone-diag", val topic: String = "altoida-phone-diag")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "TAG", "PAYLOAD", "CONTRAST", "MOVEMENT", "ANGLE", "FEATURES") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val diag = PhoneDiag(
                time,
                timeReceived,
                lineValues["TAG"]?.toString(),
                lineValues["PAYLOAD"]?.toString(),
                // lineValues["CONTRAST"]?.toFloat(),
                // lineValues["MOVEMENT"]?.toFloat(),
                // lineValues["ANGLE"]?.toFloat(),
                // lineValues["FEATURES"]?.toFloat()


        )

        return TopicData(false, topic, diag)
    }
}