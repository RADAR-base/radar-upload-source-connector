package org.radarbase.connect.upload.converter

import org.radarcns.connector.altoida.AltoidaNormalVector
import java.time.LocalDateTime
import java.time.ZoneOffset

class AltoidaNormalVectorConverter(override val sourceType: String = "version", val topic: String = "upload_altoida_meta_data")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("2019031801") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()?.toDouble()
        val version = "2019031801"
        val normalVector = AltoidaNormalVector(
                time,
                timeReceived,
                version

        )

        return TopicData(false, topic, normalVector)
    }
}
