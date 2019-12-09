package org.radarbase.connect.upload.io

import java.io.Closeable
import java.io.InputStream
import java.nio.file.Path

interface FileUploader: Closeable {
    fun upload(path: Path, stream: InputStream)
}
