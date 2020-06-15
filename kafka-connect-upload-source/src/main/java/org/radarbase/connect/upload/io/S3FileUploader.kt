package org.radarbase.connect.upload.io

import io.minio.MinioClient
import io.minio.PutObjectOptions
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.nio.file.Path

class S3FileUploader(override val config: FileUploaderFactory.FileUploaderConfig) : FileUploaderFactory.FileUploader {
    override val type: String
        get() = "s3"
    private val s3Client: MinioClient
    private val bucket: String
    init {
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

    override fun upload(path: Path, stream: InputStream, size: Long?) {
        logger.info("Uploading object ${path} to $bucket")
        val putObjectOptions = if (size == null) PutObjectOptions(-1, (5*1024*2014)) else PutObjectOptions(size, -1)
        s3Client.putObject(bucket, path.toString(), stream, putObjectOptions)
    }

    override fun close() {
    }
    
    companion object {
        private val logger = LoggerFactory.getLogger(S3FileUploader::class.java)
    }
}
