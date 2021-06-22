package org.radarbase.connect.upload.io

import org.radarbase.connect.upload.converter.RecordLogger
import org.radarbase.connect.upload.exception.ConversionFailedException
import org.radarbase.connect.upload.exception.ConversionTemporarilyFailedException
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path

class LocalFileUploader(
    override val config: FileUploaderFactory.FileUploaderConfig
) : FileUploaderFactory.FileUploader {
    override var recordLogger: RecordLogger? = null
    override val type: String
        get() = "local"

    override fun upload(relativePath: Path, stream: InputStream, size: Long?) : URI {
        val filePath = rootDirectory.resolve(relativePath)
        try {
            writeFile(filePath, stream)
        } catch (ex: NoSuchFileException) {
            recordLogger?.info("Retrying to create parent directories for $filePath")
            try {
                Files.createDirectories(filePath.parent)
                recordLogger?.info("Created parent directory for $filePath", )
                writeFile(filePath, stream)
            } catch (ex: IOException) {
                recordLogger?.error("Could not write to $filePath")
                throw ConversionTemporarilyFailedException("Could not write to $filePath", ex)
            }
        }
        return resolveTargetUri(filePath)
    }

    private fun writeFile(filePath: Path, stream: InputStream) {
        try {
            Files.newOutputStream(filePath).use {
                stream.copyTo(it)
            }
        } catch (ex: Exception) {
            recordLogger?.error("Could not upload file", ex)
            throw ex
        }
    }

    override fun close() = Unit
}
