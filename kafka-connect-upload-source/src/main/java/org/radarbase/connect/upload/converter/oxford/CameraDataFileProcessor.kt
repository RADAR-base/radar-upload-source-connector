package org.radarbase.connect.upload.converter.oxford

import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.FileProcessor
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarbase.connect.upload.converter.TimeFieldParser
import org.radarbase.connect.upload.converter.TopicData
import org.radarcns.connector.upload.oxford.OxfordCameraAxes
import org.radarcns.connector.upload.oxford.OxfordCameraData
import org.radarcns.connector.upload.oxford.OxfordCameraRgb
import java.io.InputStream
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

class CameraDataFileProcessor : FileProcessorFactory {
    override fun matches(contents: ContentsDTO) = contents.fileName.endsWith("image_table.txt")

    override fun createProcessor(record: RecordDTO): FileProcessor = CameraFileProcessor()

    private class CameraFileProcessor : FileProcessor {
        override fun processData(
            context: ConverterFactory.ContentsContext,
            inputStream: InputStream,
            produce: (TopicData) -> Unit,
        ) {
            val lines = inputStream.bufferedReader().use { it.readLines() }

            require(lines.size >= 2) { "Image table too short" }

            val header = lines[1]
                .drop(1)
                .split(",")
                .map { it.trim() }

            lines
                .asSequence()
                .drop(2)
                .map { line ->
                    TopicData(
                        TOPIC,
                        line.toOxfordCameraRecord(header, context.timeReceived),
                    )
                }
                .forEach(produce)
        }

        fun String.toOxfordCameraRecord(
            header: List<String>,
            timeReceived: Double,
        ) = split(",")
            .asSequence()
            .mapIndexed { idx, field -> header[idx] to field.trim() }
            .toMap()
            .toOxfordCameraRecord(timeReceived)

        fun Map<String, String>.toOxfordCameraRecord(
            timeReceived: Double,
        ) = OxfordCameraData(
            TIME_PARSER.time(this),
            timeReceived,
            getValue("id"),
            getValue("p") == "1",
            OxfordCameraAxes(
                getValue("accx").toFloat(),
                getValue("accy").toFloat(),
                getValue("accz").toFloat(),
            ),
            OxfordCameraAxes(
                getValue("magx").toFloat(),
                getValue("magy").toFloat(),
                getValue("magz").toFloat(),
            ),
            OxfordCameraAxes(
                getValue("xor").toFloat(),
                getValue("yor").toFloat(),
                getValue("zor").toFloat(),
            ),
            getValue("tem").toFloat(),
            OxfordCameraRgb(
                getValue("red").toInt() / RGB_RANGE,
                getValue("green").toInt() / RGB_RANGE,
                getValue("blue").toInt() / RGB_RANGE,
            ),
            getValue("lum").toInt() / LUMINANCE_RANGE,
            getValue("exp").toInt(),
            getValue("gain").toFloat(),
            OxfordCameraRgb(
                getValue("rbal").toFloat(),
                getValue("gbal").toFloat(),
                getValue("bbal").toFloat(),
            ),
        )
    }

    companion object {
        private val TIME_PARSER = TimeFieldParser.DateFormatParser(
            DateTimeFormatterBuilder()
                // date/time
                .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                // offset (hhmm - "+0000" when it's zero)
                .appendOffset("+HHMM", "+0000")
                // create formatter
                .toFormatter(),
            "dt",
        )

        private const val TOPIC = "connect_upload_oxford_camera_data"
        private const val RGB_RANGE: Float = 255f
        private const val LUMINANCE_RANGE: Float = RGB_RANGE * RGB_RANGE * RGB_RANGE
    }
}
