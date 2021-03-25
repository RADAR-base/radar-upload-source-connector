package org.radarbase.connect.upload.converter.gaitup

import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.io.FileUploaderFactory
import org.radarcns.connector.upload.physilog.PhysilogBinaryDataReference
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

open class PhysilogUploadProcessorFactory(
    private val uploaderCreate: () -> FileUploaderFactory.FileUploader
) : FileProcessorFactory {

    open fun beforeProcessing(context: ConverterFactory.ContentsContext) = Unit

    open fun afterProcessing(context: ConverterFactory.ContentsContext) = Unit

    override fun matches(contents: ContentsDTO) = SUFFIX_REGEX.containsMatchIn(contents.fileName)

    override fun createProcessor(record: RecordDTO): FileProcessorFactory.FileProcessor = PhysilogFileUploadProcessor(record)

    private inner class PhysilogFileUploadProcessor(private val record: RecordDTO) : FileProcessorFactory.FileProcessor {
        override fun processData(
            context: ConverterFactory.ContentsContext,
            inputStream: InputStream,
            produce: (TopicData) -> Unit
        ) {
            beforeProcessing(context)
            val fileName = context.fileName
            context.logger.debug("Processing $fileName")
            // Create date directory based on uploaded time.
            val dateDirectory = directoryDateFormatter.format(Instant.now())

            val projectId = checkNotNull(record.data?.projectId) { "Project ID required to upload image files." }
            val userId = checkNotNull(record.data?.userId) { "Project ID required to upload image files." }
            val relativePath = Paths.get("$projectId/$userId/$TOPIC/${record.id}/$dateDirectory/$fileName")

            try {
                val url = uploaderCreate()
                    .upload(relativePath, inputStream, context.contents.size)
                    .toString()

                context.logger.info("Uploaded file to $url")

                produce(TopicData(
                    TOPIC,
                    PhysilogBinaryDataReference(
                        context.timeReceived,
                        context.timeReceived,
                        fileName,
                        url,
                    ),
                ))
            } catch (exe: IOException) {
                context.logger.error("Could not upload file", exe)
                throw exe
            } finally {
                context.logger.info("Finalising the upload")
                afterProcessing(context)
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
