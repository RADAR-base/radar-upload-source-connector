package org.radarbase.connect.upload.converter

import RADAR-Schemas.commons.connector.altoida.AltoidaNormalVector

class AltoidaNormalVectorConverter(override val sourceType: String = "normal-vector", val topic: String = "altoida-normal-vector")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "X", "Y") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val normalVector = AltoidaNormalVector(
                time,
                timeReceived,
                lineValues["X"]?.toFloat(),
                lineValues["Y"]?.toFloat()

        )

        return TopicData(false, topic, normalVector)
    }
}