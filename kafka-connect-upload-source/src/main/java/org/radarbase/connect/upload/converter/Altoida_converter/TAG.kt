package org.radarbase.connect.upload.converter

import Altoida.avroSchemas.PhoneTagLog

class TagConverter(override val sourceType: String = "phone-tag-log", val topic: String = "altoida-phone-tag-log")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "TAG", "PAYLOAD") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val tag = PhoneTagLog(
                time,
                timeReceived,
                lineValues["TAG"]?.toString(),
                lineValues["PAYLOAD"]?.toString()

        )

        return TopicData(false, topic, tag)
    }
}