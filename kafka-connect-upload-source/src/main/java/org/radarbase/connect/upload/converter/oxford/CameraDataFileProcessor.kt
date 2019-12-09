package org.radarbase.connect.upload.converter.oxford

import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarcns.connector.upload.oxford.OxfordCameraAxes
import org.radarcns.connector.upload.oxford.OxfordCameraData
import org.radarcns.connector.upload.oxford.OxfordCameraRgb
import java.io.InputStream
import java.io.InputStreamReader
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

class CameraDataFileProcessor : FileProcessorFactory {
    override fun matches(contents: ContentsDTO) = contents.fileName.endsWith("image_table.txt")

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

                        FileProcessorFactory.TopicData(TOPIC,
                                mapValuesToRecord(values, timeReceived))
                    }
        }

        fun mapValuesToRecord(values: Map<String, String>, timeReceived: Double) = OxfordCameraData(
                TIME_PARSER.parse(values.getValue("dt"))
                        .getLong(ChronoField.INSTANT_SECONDS)
                        .toDouble(),
                timeReceived,
                values.getValue("id"),
                values.getValue("p") == "1",
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
                        values.getValue("red").toInt() / RGB_RANGE,
                        values.getValue("green").toInt() / RGB_RANGE,
                        values.getValue("blue").toInt() / RGB_RANGE
                ),
                values.getValue("lum").toInt() / LUMINANCE_RANGE,
                values.getValue("exp").toInt(),
                values.getValue("gain").toFloat(),
                OxfordCameraRgb(
                        values.getValue("rbal").toFloat(),
                        values.getValue("gbal").toFloat(),
                        values.getValue("bbal").toFloat()
                ))
    }

    companion object {
        private val TIME_PARSER = DateTimeFormatterBuilder()
                // date/time
                .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                // offset (hhmm - "+0000" when it's zero)
                .appendOffset("+HHMM", "+0000")
                // create formatter
                .toFormatter();
        private const val TOPIC = "connect_upload_oxford_camera_data"
        private const val RGB_RANGE: Float = 255f
        private const val LUMINANCE_RANGE: Float = RGB_RANGE * RGB_RANGE * RGB_RANGE
    }
}
