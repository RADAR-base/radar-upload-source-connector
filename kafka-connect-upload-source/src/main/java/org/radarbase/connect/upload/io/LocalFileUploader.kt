package org.radarbase.connect.upload.io

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

class LocalFileUploader : FileUploader {
    override fun upload(path: Path, stream: InputStream) {
        Files.newOutputStream(path).use {
            stream.copyTo(it)
        }
    }

    override fun close() = Unit
}
