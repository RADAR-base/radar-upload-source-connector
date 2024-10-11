package org.radarbase.connect.upload.converter.archive

import org.apache.commons.compress.archivers.ArchiveEntry
import java.io.Closeable
import java.io.InputStream

interface ArchiveIterator : Closeable {
    fun files(
        entryFilter: (ArchiveEntry) -> Boolean = { true },
    ): Sequence<Pair<ArchiveEntry, InputStream>>
}
