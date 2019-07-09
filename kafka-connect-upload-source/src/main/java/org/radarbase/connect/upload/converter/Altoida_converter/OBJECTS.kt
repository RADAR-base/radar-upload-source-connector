package org.radarbase.connect.upload.converter

import Altoida.avroSchemas.PhoneObject

class ObjectConverter(override val sourceType: String = "phone-object", val topic: String = "altoida-phone-object ")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "OBJECT", "X", "Y", "Z") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val obj = PhoneObject(
                time,
                timeReceived,
                lineValues["OBJECT"]?.toString(),
                lineValues["X"]?.toFloat(),
                lineValues["Y"]?.toFloat(),
                lineValues["Z"]?.toFloat(),
        )

        return TopicData(false, topic, obj)
    }
}