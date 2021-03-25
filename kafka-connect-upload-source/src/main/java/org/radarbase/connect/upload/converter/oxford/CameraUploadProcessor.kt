package org.radarbase.connect.upload.converter.oxford

import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarbase.connect.upload.converter.LogRepository
import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.io.FileUploaderFactory
import org.radarcns.connector.upload.oxford.OxfordCameraImage
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.nio.file.Paths
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField

class CameraUploadProcessor(
        private val logRepository: LogRepository,
        private val uploaderCreate: () -> FileUploaderFactory.FileUploader
) : FileProcessorFactory {
    override fun matches(contents: ContentsDTO) = SUFFIX_REGEX.containsMatchIn(contents.fileName)

    override fun createProcessor(record: RecordDTO): FileProcessorFactory.FileProcessor = FileUploadProcessor(record)

    private inner class FileUploadProcessor(
        private val record: RecordDTO,
    ) : FileProcessorFactory.FileProcessor {
        private val recordLogger = logRepository.createLogger(logger, record.id!!)

        override fun processData(
            context: ConverterFactory.ContentsContext,
            inputStream: InputStream,
            timeReceived: Double,
            produce: (TopicData) -> Unit
        ) {
            val fileName = context.fileName.split('/').last()
            val adjustedFilename = SUFFIX_REGEX.replace(fileName, "")
            val formattedTime = checkNotNull(FILENAME_REGEX.matchEntire(fileName)) { "Image file name $fileName does not match pattern" }
                    .groupValues[1]
            val dateTime = fileDateFormatter.parse(formattedTime)
            val time = dateTime.getLong(ChronoField.INSTANT_SECONDS).toDouble()
            val dateDirectory = directoryDateFormatter.format(dateTime)

            val projectId = checkNotNull(record.data?.projectId) { "Project ID required to upload image files." }
            val userId = checkNotNull(record.data?.userId) { "Project ID required to upload image files." }
            val relativePath = Paths.get("$projectId/$userId/$TOPIC/${record.id}/$dateDirectory/$adjustedFilename.jpg")

            val url = uploaderCreate().upload(relativePath, inputStream, context.contents.size)

            produce(TopicData(
                TOPIC,
                OxfordCameraImage(time, timeReceived, adjustedFilename, url.toString())
                    .also { recordLogger.info("Uploaded file to ${it.getUrl()}") },
            ))
        }
    }

    companion object {
        private const val TOPIC = "connect_upload_oxford_camera_image"

        private val SUFFIX_REGEX = Regex("\\.jpeg|\\.res|\\.jpg$", RegexOption.IGNORE_CASE)
        private val FILENAME_REGEX = Regex("^[^_]+_[^_]+_([0-9]+_[0-9]+)E.*")
        private val fileDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneId.of("UTC"))
        private val directoryDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(ZoneId.of("UTC"))

        private val logger = LoggerFactory.getLogger(CameraUploadProcessor::class.java)
    }
}
