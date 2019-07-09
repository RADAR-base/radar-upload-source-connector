package org.radarbase.connect.upload.converter

import Altoida.avroSchemas.PhoneNormalVector

class NormalVectorConverter(override val sourceType: String = "phone-normalVector", val topic: String = "altoida-phone-normalVector")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "X", "Y") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val normalVector = PhoneNormalVector(
                time,
                timeReceived,
                lineValues["X"]?.toFloat(),
                lineValues["Y"]?.toFloat()

        )

        return TopicData(false, topic, normalVector)
    }
}