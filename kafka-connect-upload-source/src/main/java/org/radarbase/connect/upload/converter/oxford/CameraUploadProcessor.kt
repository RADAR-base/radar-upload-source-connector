package org.radarbase.connect.upload.converter.oxford

import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarbase.connect.upload.io.SftpFileUploader
import org.radarcns.connector.upload.oxford.OxfordCameraImage
import java.io.InputStream
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField

class CameraUploadProcessor(private val sftpCredentials: SftpFileUploader.SftpCredentials, private val rootPath: Path, private val advertisedUrl: URL) : FileProcessorFactory {
    override fun matches(contents: ContentsDTO) = SUFFIX_REGEX.matches(contents.fileName)

    override fun createProcessor(record: RecordDTO): FileProcessorFactory.FileProcessor = FileUploadProcessor(record)

    private inner class FileUploadProcessor(private val record: RecordDTO) : FileProcessorFactory.FileProcessor {
        override fun processData(contents: ContentsDTO, inputStream: InputStream, timeReceived: Double): List<FileProcessorFactory.TopicData> {
            val fileName = contents.fileName.split('/').last()
            val adjustedFilename = SUFFIX_REGEX.replace(fileName, "")
            val formattedTime = checkNotNull(FILENAME_REGEX.matchEntire(fileName)) { "Image file name does not match pattern" }
                    .groupValues[1]
            val dateTime = fileDateFormatter.parse(formattedTime)
            val time = dateTime.getLong(ChronoField.INSTANT_SECONDS).toDouble()
            val dateDirectory = directoryDateFormatter.format(dateTime)

            val projectId = checkNotNull(record.data?.projectId) { "Project ID required to upload image files." }
            val userId = checkNotNull(record.data?.userId) { "Project ID required to upload image files." }
            val relativePath = Paths.get("$projectId/$userId/$topic/$dateDirectory/$adjustedFilename.jpg")
            val sftpPath = rootPath.resolve(relativePath).normalize()

            SftpFileUploader(sftpCredentials).use { sftp ->
                sftp.upload(sftpPath, inputStream)
            }

            val url = URL(advertisedUrl, relativePath.toString())
            return listOf(FileProcessorFactory.TopicData(topic,
                    OxfordCameraImage(time, timeReceived, adjustedFilename, url.toString())))
        }
    }

    companion object {
        private const val topic = "connect_upload_oxford_camera_image"

        private val SUFFIX_REGEX = Regex("(\\.jpeg|\\.res|\\.jpg)$", RegexOption.IGNORE_CASE)
        private val FILENAME_REGEX = Regex("^[^_]+_[^_]+([0-9]{6}+_[0-9]{6}).*")
        private val fileDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneId.of("UTC"))
        private val directoryDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(ZoneId.of("UTC"))
    }
}
