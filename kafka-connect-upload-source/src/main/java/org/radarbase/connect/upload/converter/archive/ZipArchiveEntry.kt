package org.radarbase.connect.upload.converter.archive

import org.apache.commons.compress.archivers.ArchiveEntry
import java.util.*
import java.util.zip.ZipEntry

class ZipArchiveEntry(val entry: ZipEntry): ArchiveEntry {
    override fun getName(): String = entry.name
    override fun getSize(): Long = entry.size
    override fun isDirectory(): Boolean = entry.isDirectory
    override fun getLastModifiedDate(): Date = Date(entry.lastModifiedTime.toMillis())
}
