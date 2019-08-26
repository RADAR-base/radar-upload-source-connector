package org.radarbase.connect.upload.converter.altoida

import org.radarbase.connect.upload.converter.CsvRecordConverter
import org.radarbase.connect.upload.converter.TopicData
import org.radarcns.connector.altoida.UploadAltoidaMetadata
import java.time.LocalDateTime
import java.time.ZoneOffset

class MetadataCsvRecordConverter(override val sourceType: String = "version", val topic: String = "connect_upload_altoida_metadata")
    : CsvRecordConverter(sourceType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("2019031801") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()?.toDouble()
        val version = "2019031801"
        val metadata = UploadAltoidaMetadata(
                time,
                timeReceived,
                version

        )

        return TopicData(false, topic, metadata)
    }
}
