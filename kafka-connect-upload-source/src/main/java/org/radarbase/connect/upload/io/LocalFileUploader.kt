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


    override fun upload(path: Path, stream: InputStream, size: Long?) {
        try {
            Files.newOutputStream(path).use {
                stream.copyTo(it)
            }
        } catch (ex: NoSuchFileException) {
            logger.error("Could not write file", ex)
            logger.info("Retrying to create parent directories for ${path.toUri()}")
            if(File(path.toUri()).parentFile.mkdirs()) {
                logger.info("Created parent directory for ${path.toUri()}")
                try {
                    Files.newOutputStream(path).use {
                        stream.copyTo(it)
                    }
                } catch (ex: Exception) {
                    logger.error("Could not write file", ex)
                    throw ex
                }

            } else {
                logger.error("Could not write to ${path.toUri()}")
                throw ConversionFailedException("Could not write to ${path.toUri()}", ex)
            }

        } catch (ex: Exception) {
            logger.error("Could not upload file", ex)
            throw ex
        }
    }

    override fun close() = Unit

    companion object {
        private val logger = LoggerFactory.getLogger(LocalFileUploader::class.java)
    }
}
