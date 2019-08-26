package org.radarbase.connect.upload.converter.altoida

import okhttp3.ResponseBody
import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.LogLevel
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.RecordConverter
import org.radarbase.connect.upload.converter.TopicData
import org.radarcns.connector.upload.altoida.AltoidaMetadata
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset

class AltoidaMetadataConverter(override val sourceType: String = "altoida_metadata", val topic: String = "connect_upload_altoida_metadata")
    : RecordConverter(sourceType) {

    override fun processData(contents: ContentsDTO, responseBody: ResponseBody, record: RecordDTO, timeReceived: Double): List<TopicData> {
        log(LogLevel.INFO,"Retrieved file content from record id ${record.id} and filename ${contents.fileName}")
        val inputStream = responseBody.byteStream()
        val reader = inputStream.bufferedReader()
        try {
            val version = reader.readLine()
            val convertedLine = convertVersionToRecord(version, timeReceived)
            if (convertedLine != null) {
                return listOf(convertedLine)
            }
        } catch (exe: IOException) {
            log(LogLevel.WARN,"Something went wrong while processing contents of file ${contents.fileName}: ${exe.message} ")
        } finally {
            log(LogLevel.INFO,"Closing resources of content ${contents.fileName}")
            inputStream.close()
            responseBody.close()
        }
        return emptyList()
    }

    private fun convertVersionToRecord(version: String, timeReceived: Double): TopicData? {
        val time = LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()?.toDouble()
        val metadata = AltoidaMetadata(
                time,
                timeReceived,
                version
        )
        return TopicData(true, topic, metadata)
    }
}
