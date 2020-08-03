package org.radarbase.connect.upload.io

import io.minio.*
import io.minio.errors.MinioException
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.net.ConnectException
import java.net.URI
import java.nio.file.Path

class S3FileUploader(override val config: FileUploaderFactory.FileUploaderConfig) : FileUploaderFactory.FileUploader {
    override val type: String = "s3"
    private val s3Client: MinioClient
    private val bucket: String
    init {
        if (config.targetEndpoint.isEmpty()) throw ConnectException("upload.source.file.target.endpoint should have a valid url of an S3 storage")
        config.username ?: throw ConnectException("upload.source.file.uploader.username must be configured for one or more of the selected converters")
        config.password ?: throw ConnectException("upload.source.file.uploader.password must be configured for one or more of the selected converters")

        try {
            s3Client = try {
                MinioClient.Builder()
                        .endpoint(config.targetEndpoint)
                        .credentials(config.username, config.password)
                        .build()
            } catch (ex: IllegalArgumentException) {
                logger.warn("Invalid S3 configuration provided for $config.targetEndpoint", ex)
                throw ex
            }
            bucket = config.targetRoot
            logger.info("Object storage configured with endpoint {} in bucket {}",
                    config.targetEndpoint, bucket)

            // Check if the bucket already exists.
            val isExist: Boolean = s3Client.bucketExists(BucketExistsArgs.Builder()
                    .bucket(bucket)
                    .build())
            if (isExist) {
                logger.info("Bucket $bucket already exists.")
            } else {
                s3Client.makeBucket(MakeBucketArgs.Builder()
                        .bucket(bucket)
                        .build())
                logger.info("Bucket $bucket was created.")
            }
        } catch (ex: MinioException) {
            throw IOException(ex)
        }
    }

    override fun upload(relativePath: Path, stream: InputStream, size: Long?) : URI {
        logger.info("Uploading object $relativePath to $bucket")
        try {
            s3Client.putObject(PutObjectArgs.Builder()
                    .bucket(bucket)
                    .`object`(relativePath.toString())
                    .stream(stream, size ?: -1, 5 * 1024 * 2014)
                    .build())
        } catch (ex: MinioException) {
            throw IOException(ex)
        }

        return advertisedTargetUri().resolve(rootDirectory().resolve(relativePath).toString())
    }

    override fun close() {
    }

    companion object {
        private val logger = LoggerFactory.getLogger(S3FileUploader::class.java)
    }
}
