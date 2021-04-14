package org.radarbase.connect.upload.io

import org.apache.kafka.connect.errors.ConnectException
import org.radarbase.connect.upload.UploadSourceConnectorConfig
import java.io.Closeable
import java.io.InputStream
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths


class FileUploaderFactory(private val config: Map<String, String>) {
    /**
     * Create a line processor for given record.
     */
    fun fileUploader(): FileUploader {
        val connectorConfig = UploadSourceConnectorConfig(config)
        val uploadType = connectorConfig.fileUploaderType
        val uploaderConfig = connectorConfig.fileUploadConfig
        return when (uploadType) {
            UploadType.LOCAL -> LocalFileUploader(uploaderConfig)
            UploadType.SFTP -> SftpFileUploader(uploaderConfig)
            UploadType.S3 -> S3FileUploader(uploaderConfig)
        }
    }

    interface FileUploader: Closeable {
        val type: String

        val config: FileUploaderConfig

        fun advertisedTargetUri(): URI = URI(if (config.targetEndpoint.endsWith("/")) config.targetEndpoint else "${config.targetEndpoint}/")

        fun rootDirectory(): Path = Paths.get(config.targetRoot.ifEmpty {"."})

        fun upload(relativePath: Path, stream: InputStream, size: Long?) : URI

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
