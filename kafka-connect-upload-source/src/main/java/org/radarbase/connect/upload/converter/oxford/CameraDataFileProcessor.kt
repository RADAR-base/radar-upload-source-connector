package org.radarbase.connect.upload.converter.oxford

import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarcns.connector.upload.oxford.OxfordCameraAxes
import org.radarcns.connector.upload.oxford.OxfordCameraData
import org.radarcns.connector.upload.oxford.OxfordCameraRgb
import java.io.InputStream
import java.io.InputStreamReader
import java.time.ZonedDateTime
import java.time.temporal.ChronoField

class CameraDataFileProcessor : FileProcessorFactory {
    override fun matches(contents: ContentsDTO) = contents.fileName == "image_table.txt"

    override fun createProcessor(record: RecordDTO): FileProcessorFactory.FileProcessor = CameraFileProcessor()

    private class CameraFileProcessor : FileProcessorFactory.FileProcessor {
        override fun processData(contents: ContentsDTO, inputStream: InputStream, timeReceived: Double): List<FileProcessorFactory.TopicData> {
            val lines = InputStreamReader(inputStream).use { it.readLines() }

            require(lines.size >= 2) { "Image table too short" }

            val header = lines[1].substring(1).split(",").map { it.trim() }

            return lines.subList(2, lines.size)
                    .map { line ->
                        val values = line
                                .split(",")
                                .mapIndexed { idx, field -> header[idx] to field.trim() }
                                .toMap()

                        FileProcessorFactory.TopicData(topic,
                                mapValuesToRecord(values, timeReceived))
                    }
        }

        fun mapValuesToRecord(values: Map<String, String>, timeReceived: Double) = OxfordCameraData(
                ZonedDateTime.parse(values["dt"]).getLong(ChronoField.INSTANT_SECONDS).toDouble(),
                timeReceived,
                values.getValue("id"),
                OxfordCameraAxes(
                        values.getValue("accx").toFloat(),
                        values.getValue("accy").toFloat(),
                        values.getValue("accz").toFloat()),
                OxfordCameraAxes(
                        values.getValue("magx").toFloat(),
                        values.getValue("magy").toFloat(),
                        values.getValue("magz").toFloat()),
                OxfordCameraAxes(
                        values.getValue("xor").toFloat(),
                        values.getValue("yor").toFloat(),
                        values.getValue("zor").toFloat()),
                values.getValue("tem").toFloat(),
                OxfordCameraRgb(
                        values.getValue("red").toInt() / 255.0f,
                        values.getValue("green").toInt() / 255.0f,
                        values.getValue("blue").toInt() / 255.0f
                ),
                OxfordCameraRgb(
                        values.getValue("rbal").toFloat(),
                        values.getValue("gbal").toFloat(),
                        values.getValue("bbal").toFloat()
                ),
                values.getValue("lum").toInt(),
                values.getValue("exp").toInt(),
                values.getValue("gain").toFloat())
    }

    companion object {
        private const val topic = "connect_upload_oxford_camera_data"
    }
}
