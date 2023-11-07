package org.radarbase.connect.upload.io

import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path

class TempFile(
    tempDir: Path,
    prefix: String,
) : Closeable {
    val tempFile: Path = Files.createTempFile(tempDir, prefix, ".tmp")

    fun outputStream(): OutputStream = Files.newOutputStream(tempFile)

    fun inputStream(): InputStream = Files.newInputStream(tempFile)

    override fun close() {
        Files.delete(tempFile)
    }

    companion object {
        fun InputStream.copyToTempFile(
            tempDir: Path,
            prefix: String,
        ): TempFile = use {
            val tempFile = TempFile(tempDir, prefix)
            copyTo(tempFile.outputStream())
            return tempFile
        }
    }
}
