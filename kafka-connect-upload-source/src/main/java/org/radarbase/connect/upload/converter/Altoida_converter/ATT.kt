package org.radarbase.connect.upload.converter

import Altoida.avroSchemas.PhoneAttitude

class AttitudeConverter(override val sourceType: String = "phone-attitude", val topic: String = "altoida-phone-attitude ")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "PITCH", "ROLL", "YAW") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val attitude = PhoneAttitude(
                time,
                timeReceived,
                lineValues["PITCH"]?.toFloat(),
                lineValues["ROLL"]?.toFloat(),
                lineValues["YAW"]?.toFloat()
        )

        return TopicData(false, topic, attitude)
    }
}