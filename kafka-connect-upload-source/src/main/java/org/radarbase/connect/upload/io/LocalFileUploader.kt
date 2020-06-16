package org.radarbase.connect.upload.io

import org.radarbase.connect.upload.exception.ConversionFailedException
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path

class LocalFileUploader( override val config: FileUploaderFactory.FileUploaderConfig) : FileUploaderFactory.FileUploader {
    override val type: String
        get() = "local"


    override fun upload(relativePath: Path, stream: InputStream, size: Long?) : URI {
        val filePath = rootDirectory().resolve(relativePath)
        try {
            Files.newOutputStream(filePath).use {
                stream.copyTo(it)
            }
        } catch (ex: NoSuchFileException) {
            logger.error("Could not write file", ex)
            logger.info("Retrying to create parent directories for ${filePath.toUri()}")
            if(File(filePath.toUri()).parentFile.mkdirs()) {
                logger.info("Created parent directory for ${filePath.toUri()}")
                try {
                    Files.newOutputStream(filePath).use {
                        stream.copyTo(it)
                    }
                } catch (ex: Exception) {
                    logger.error("Could not write file", ex)
                    throw ex
                }

            } else {
                logger.error("Could not write to ${filePath.toUri()}")
                throw ConversionFailedException("Could not write to ${filePath.toUri()}", ex)
            }

        } catch (ex: Exception) {
            logger.error("Could not upload file", ex)
            throw ex
        }
        return advertisedTargetUri().resolve(filePath.toString())
    }

    override fun close() = Unit

    companion object {
        private val logger = LoggerFactory.getLogger(LocalFileUploader::class.java)
    }
}
