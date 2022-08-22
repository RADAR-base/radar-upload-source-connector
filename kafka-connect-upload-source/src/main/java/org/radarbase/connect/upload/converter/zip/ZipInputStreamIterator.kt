package org.radarbase.connect.upload.converter.zip

import java.io.Closeable
import java.io.FilterInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ZipInputStreamIterator(
    input: InputStream,
): Closeable {
    private val zipInputStream = ZipInputStream(input)

    fun files(
        entryFilter: (ZipEntry) -> Boolean = { true },
    ): Sequence<Pair<ZipEntry, InputStream>> =
        generateSequence { zipInputStream.nextEntry }
            .filter { zipEntry -> !zipEntry.isDirectory && entryFilter(zipEntry) }
            .map { zipEntry -> zipEntry to EntryInputStream() }

    override fun close() = zipInputStream.close()

    inner class EntryInputStream : FilterInputStream(zipInputStream) {
        override fun close() = zipInputStream.closeEntry()
    }
}
