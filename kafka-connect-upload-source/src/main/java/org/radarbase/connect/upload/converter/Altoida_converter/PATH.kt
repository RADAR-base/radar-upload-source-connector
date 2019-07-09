package org.radarbase.connect.upload.converter

import Altoida.avroSchemas.PhonePath

class PathConverter(override val sourceType: String = "phone-path", val topic: String = "altoida-phone-path")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "X", "Y", "Z") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val path = PhonePath(
                time,
                timeReceived,
                lineValues["X"]?.toFloat(),
                lineValues["Y"]?.toFloat(),
                lineValues["Z"]?.toFloat()

        )

        return TopicData(false, topic, path)
    }
}