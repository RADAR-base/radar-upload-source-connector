package org.radarbase.connect.upload.io

import org.apache.kafka.connect.errors.ConnectException
import org.radarbase.connect.upload.UploadSourceConnectorConfig
import org.radarbase.connect.upload.api.RecordDTO
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.InputStream
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


class FileUploaderFactory(private val config: Map<String, String>) {
    /**
     * Create a line processor for given record.
     */
    fun fileUploader(): FileUploader {
        val connectorConfig = UploadSourceConnectorConfig(config)
        val uploadType = connectorConfig.fileUploaderType ?: throw ConnectException("FileUploader class is required by one or more of registered converters.")
        val uploaderConfig = connectorConfig.fileUploadConfig
        return when (uploadType) {
            UploadType.LOCAL -> LocalFileUploader(uploaderConfig)
            UploadType.SFTP -> SftpFileUploader(uploaderConfig)
            UploadType.S3 -> S3FileUploader(uploaderConfig)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LocalFileUploader::class.java)


        private val directoryDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(ZoneId.of("UTC"))

        fun resolveUploadPath(fileName: String, topic: String, config: FileUploaderConfig, record: RecordDTO) : Path {
            logger.debug("Processing $fileName")
            val root = Paths.get(config.targetRoot)
            // Create date directory based on uploaded time.
            val dateDirectory = directoryDateFormatter.format(Instant.now())

            val projectId = checkNotNull(record.data?.projectId) { "Project ID required to upload image files." }
            val userId = checkNotNull(record.data?.userId) { "Project ID required to upload image files." }
            val relativePath = Paths.get("$projectId/$userId/$topic/${record.id}/$dateDirectory/$fileName")
            return root.resolve(relativePath).normalize()

        }
    }

    interface FileUploader: Closeable {
        val type: String

        val config: FileUploaderConfig

        fun advertisedTargetUri(): URI = URI(if (config.targetEndpoint.endsWith("/")) config.targetEndpoint else "${config.targetEndpoint}/")

        fun rootDirectory(): Path = Paths.get(config.targetRoot.ifEmpty {"."})

        fun upload(path: Path, stream: InputStream, size: Long?)

    }

    data class FileUploaderConfig(
        val targetEndpoint : String,
        val targetRoot: String,
        val username: String?,
        val password: String?,
        val sshPrivateKey: String?,
        val sshPassPhrase: String?

    )

}

enum class UploadType {
    LOCAL, SFTP, S3
}
