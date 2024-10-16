package org.radarbase.connect.upload.converter.oxford

import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.FileProcessor
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.io.FileUploaderFactory
import org.radarcns.connector.upload.oxford.OxfordCameraImage
import java.io.InputStream
import java.nio.file.Paths
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.TemporalAccessor

class CameraUploadProcessor(
    private val uploaderSupplier: () -> FileUploaderFactory.FileUploader,
) : FileProcessorFactory {
    override fun matches(contents: ContentsDTO) = SUFFIX_REGEX.containsMatchIn(contents.fileName)

    override fun createProcessor(record: RecordDTO): FileProcessor = FileUploadProcessor(record)

    private inner class FileUploadProcessor(
        private val record: RecordDTO,
    ) : FileProcessor {
        override fun processData(
            context: ConverterFactory.ContentsContext,
            inputStream: InputStream,
            produce: (TopicData) -> Unit,
        ) {
            val fileName = context.fileName.split('/').last()
            val adjustedFilename = SUFFIX_REGEX.replace(fileName, "")
            val dateTime = fileName.extractDateFromFilename()
            val time = dateTime.getLong(ChronoField.INSTANT_SECONDS).toDouble()
            val dateDirectory = directoryDateFormatter.format(dateTime)

            val projectId = checkNotNull(record.data?.projectId) { "Project ID required to upload image files." }
            val userId = checkNotNull(record.data?.userId) { "Project ID required to upload image files." }
            val relativePath = Paths.get("$projectId/$userId/$TOPIC/${record.id}/$dateDirectory/$adjustedFilename.jpg")

            val url = uploaderSupplier()
                .upload(relativePath, inputStream, context.contents.size)
                .toString()

            context.logger.info("Uploaded file to $url")

            produce(
                TopicData(
                    TOPIC,
                    OxfordCameraImage(time, context.timeReceived, adjustedFilename, url),
                ),
            )
        }
    }

    fun String.extractDateFromFilename(): TemporalAccessor =
        checkNotNull(FILENAME_REGEX.matchEntire(this)) { "Image file name $this does not match pattern" }
            .let { fileDateFormatter.parse(it.groupValues[1]) }

    companion object {
        private const val TOPIC = "connect_upload_oxford_camera_image"

        private val SUFFIX_REGEX = Regex("\\.jpeg|\\.res|\\.jpg$", RegexOption.IGNORE_CASE)
        private val FILENAME_REGEX = Regex("^[^_]+_[^_]+_([0-9]+_[0-9]+)E.*")
        private val fileDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .withZone(ZoneId.of("UTC"))
        private val directoryDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.of("UTC"))
    }
}
