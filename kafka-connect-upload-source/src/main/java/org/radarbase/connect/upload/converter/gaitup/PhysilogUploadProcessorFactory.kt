package org.radarbase.connect.upload.converter.gaitup

import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarbase.connect.upload.converter.LogRepository
import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.io.FileUploader
import org.radarcns.connector.upload.physilog.PhysilogBinaryData
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

open class PhysilogUploadProcessorFactory(
        private val logRepository: LogRepository,
        private val uploaderCreate: () -> FileUploader,
        private val rootPath: Path,
        private val advertisedUrl: URI) : FileProcessorFactory {

    open fun beforeProcessing(contents: ContentsDTO) = Unit

    open fun afterProcessing(contents: ContentsDTO) = Unit

    override fun matches(contents: ContentsDTO) = SUFFIX_REGEX.containsMatchIn(contents.fileName)

    override fun createProcessor(record: RecordDTO): FileProcessorFactory.FileProcessor = PhysilogFileUploadProcessor(record)

    private inner class PhysilogFileUploadProcessor(private val record: RecordDTO) : FileProcessorFactory.FileProcessor {
        private val recordLogger = logRepository.createLogger(logger, record.id!!)

        override fun processData(contents: ContentsDTO, inputStream: InputStream, timeReceived: Double): List<TopicData> {
            beforeProcessing(contents)
            val fileName = contents.fileName
            logger.debug("Processing $fileName")
            // Create date directory based on uploaded time.
            val dateDirectory = directoryDateFormatter.format(Instant.now())

            val projectId = checkNotNull(record.data?.projectId) { "Project ID required to upload image files." }
            val userId = checkNotNull(record.data?.userId) { "Project ID required to upload image files." }
            val relativePath = Paths.get("$projectId/$userId/$TOPIC/${record.id}/$dateDirectory/$fileName")
            val fullPath = rootPath.resolve(relativePath).normalize()
//            val url = advertisedUrl.resolve(fullPath.toString())

            try {
                uploaderCreate().upload(fullPath, inputStream)

                return listOf(TopicData(TOPIC,
                        PhysilogBinaryData(timeReceived, timeReceived, fileName, fullPath.toString())
                                .also { recordLogger.info("Uploaded file to ${it.getUrl()}") }))
            } catch (exe: IOException) {
                logger.error("Could not upload file")
                throw exe
            } finally {
                logger.info("Finalising the upload")
                afterProcessing(contents)
            }

        }
    }

    companion object {
        private const val TOPIC = "connect_upload_physilog_binary_data"

        private val SUFFIX_REGEX = Regex("\\.zip|\\.tar.gz$", RegexOption.IGNORE_CASE)
        private val directoryDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(ZoneId.of("UTC"))

        private val logger = LoggerFactory.getLogger(PhysilogUploadProcessorFactory::class.java)
    }
}
