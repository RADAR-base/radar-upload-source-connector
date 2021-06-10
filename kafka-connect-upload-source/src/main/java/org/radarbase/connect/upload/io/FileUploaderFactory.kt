package org.radarbase.connect.upload.io

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.radarbase.connect.upload.UploadSourceConnectorConfig
import org.radarbase.connect.upload.converter.RecordLogger
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
        var recordLogger: RecordLogger?

        val type: String

        val config: FileUploaderConfig

        val advertisedTargetUri: URI
            get() = URI(if (config.targetEndpoint.endsWith("/")) config.targetEndpoint else "${config.targetEndpoint}/")

        fun resolveTargetUri(
            path: Path
        ): URI {
            val uri = advertisedTargetUri
            val originalScheme = uri.scheme
            val noRootPath = if (path.toString().startsWith("/")) Paths.get(path.toString().substring(1)) else path
            val newUri = config.targetEndpoint.replaceFirst(originalScheme, "http")
                .toHttpUrl().newBuilder()
                .addPathSegments(noRootPath.toString())
                .build()
                .toString()
            return URI(newUri.replaceFirst("http", originalScheme))
        }

        val rootDirectory: Path
            get() = Paths.get(config.targetRoot.ifEmpty {"."})

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
