package org.radarbase.connect.upload.io

import io.minio.MinioClient
import io.minio.PutObjectOptions
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.ConnectException
import java.net.URI
import java.nio.file.Path

class S3FileUploader(override val config: FileUploaderFactory.FileUploaderConfig) : FileUploaderFactory.FileUploader {
    override val type: String
        get() = "s3"
    private val s3Client: MinioClient
    private val bucket: String
    init {
        if (config.targetEndpoint.isEmpty()) throw ConnectException("upload.source.file.target.endpoint should have a valid url of an S3 storage")
        config.username ?: throw ConnectException("upload.source.file.uploader.username must be configured for one or more of the selected converters")
        config.password ?: throw ConnectException("upload.source.file.uploader.password must be configured for one or more of the selected converters")

        s3Client = try {
            MinioClient(config.targetEndpoint, config.username, config.password)
        } catch (ex: IllegalArgumentException) {
            logger.warn("Invalid S3 configuration provided for $config.targetEndpoint", ex)
            throw ex
        }
        bucket = config.targetRoot
        logger.info("Object storage configured with endpoint {} in bucket {}",
                config.targetEndpoint, bucket)

        // Check if the bucket already exists.
        val isExist: Boolean = s3Client.bucketExists(bucket)
        if (isExist) {
            logger.info("Bucket $bucket already exists.")
        } else {
            s3Client.makeBucket(bucket)
            logger.info("Bucket $bucket was created.")
        }
    }

    override fun upload(relativePath: Path, stream: InputStream, size: Long?) : URI {
        logger.info("Uploading object ${relativePath} to $bucket")
        val putObjectOptions = if (size == null) PutObjectOptions(-1, (5*1024*2014)) else PutObjectOptions(size, -1)
        s3Client.putObject(bucket, relativePath.toString(), stream, putObjectOptions)
        return advertisedTargetUri().resolve(rootDirectory().resolve(relativePath).toString())
    }

    override fun close() {
    }
    
    companion object {
        private val logger = LoggerFactory.getLogger(S3FileUploader::class.java)
    }
}
