package org.radarbase.connect.upload.converter.gaitup

import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarbase.connect.upload.converter.LogRepository
import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.io.FileUploaderFactory
import org.radarbase.connect.upload.io.S3FileUploader
import org.radarcns.connector.upload.physilog.PhysilogBinaryDataReference
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

open class PhysilogUploadProcessorFactory(
        private val logRepository: LogRepository,
        private val uploaderCreate: () -> FileUploaderFactory.FileUploader
) : FileProcessorFactory {

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
            val fullPath = uploaderCreate().rootDirectory().resolve(relativePath).normalize()
            val url = uploaderCreate().advertisedTargetUri().resolve(fullPath.toString())

            try {
                if(uploaderCreate() is S3FileUploader) {
                    uploaderCreate().upload(relativePath, inputStream, contents.size)
                }
                else {
                    uploaderCreate().upload(fullPath, inputStream, contents.size)
                }
                return listOf(TopicData(TOPIC,
                        PhysilogBinaryDataReference(timeReceived, timeReceived, fileName, url.toString())
                                .also { recordLogger.info("Uploaded file to ${it.getUrl()}") }))
            } catch (exe: IOException) {
                logger.error("Could not upload file", exe)
                throw exe
            } finally {
                logger.info("Finalising the upload")
                afterProcessing(contents)
            }

        }
    }

    companion object {
        private const val TOPIC = "connect_upload_physilog_binary_data_reference"

        private val SUFFIX_REGEX = Regex("\\.zip|\\.tar.gz$", RegexOption.IGNORE_CASE)
        private val directoryDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(ZoneId.of("UTC"))

        private val logger = LoggerFactory.getLogger(PhysilogUploadProcessorFactory::class.java)
    }
}
