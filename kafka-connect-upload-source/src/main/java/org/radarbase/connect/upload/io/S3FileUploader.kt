package org.radarbase.connect.upload.io

import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.errors.MinioException
import org.radarbase.connect.upload.logging.RecordLogger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.net.ConnectException
import java.net.URI
import java.nio.file.Path

class S3FileUploader(
    override val config: FileUploaderFactory.FileUploaderConfig,
) : FileUploaderFactory.FileUploader {
    override var recordLogger: RecordLogger? = null
    override val type: String = "s3"
    private val s3Client: MinioClient
    private val bucket: String

    init {
        try {
            s3Client = buildClient(config)
            bucket = config.targetRoot
            logger.info(
                "Object storage configured with endpoint {} in bucket {}",
                config.targetEndpoint,
                bucket,
            )

            ensureBucketExists()
        } catch (ex: MinioException) {
            throw IOException(ex)
        }
    }

    private fun ensureBucketExists() {
        // Check if the bucket already exists.
        val isExist: Boolean = s3Client.bucketExists(
            BucketExistsArgs.Builder()
                .bucket(bucket)
                .build(),
        )
        if (isExist) {
            logger.info("Bucket {} already exists.", bucket)
        } else {
            s3Client.makeBucket(
                MakeBucketArgs.Builder()
                    .bucket(bucket)
                    .build(),
            )
            logger.info("Bucket $bucket was created.")
        }
    }

    override fun upload(relativePath: Path, stream: InputStream, size: Long?): URI {
        recordLogger?.info("Uploading object $relativePath to $bucket")
        try {
            s3Client.putObject(
                PutObjectArgs.Builder()
                    .bucket(bucket)
                    .`object`(relativePath.toString())
                    .stream(stream, size ?: -1, 5 * 1024 * 2014)
                    .build(),
            )
        } catch (ex: MinioException) {
            throw IOException(ex)
        }

        return resolveTargetUri(rootDirectory.resolve(relativePath))
    }

    override fun close() = Unit

    companion object {
        private val logger = LoggerFactory.getLogger(S3FileUploader::class.java)

        private fun buildClient(config: FileUploaderFactory.FileUploaderConfig): MinioClient {
            if (config.targetEndpoint.isEmpty()) throw ConnectException("upload.source.file.target.endpoint should have a valid url of an S3 storage")
            val user = config.username ?: throw ConnectException("upload.source.file.uploader.username must be configured for one or more of the selected converters")
            val password = config.password ?: throw ConnectException("upload.source.file.uploader.password must be configured for one or more of the selected converters")

            return try {
                MinioClient.Builder()
                    .endpoint(config.targetEndpoint)
                    .credentials(user, password)
                    .build()
            } catch (ex: IllegalArgumentException) {
                logger.warn("Invalid S3 configuration provided for {}", config.targetEndpoint, ex)
                throw ex
            }
        }
    }
}
