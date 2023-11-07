package org.radarbase.connect.upload.converter.archive

import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.radarbase.connect.upload.exception.ConversionFailedException
import org.radarbase.connect.upload.io.TempFile
import org.radarbase.connect.upload.io.TempFile.Companion.copyToTempFile
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path

class SevenZipInputStreamIterator(
    input: InputStream,
    tempDir: Path,
) : ArchiveIterator {
    private val file: TempFile
    private val sevenZFile: SevenZFile

    init {
        file = input.copyToTempFile(tempDir, "7zip")
        input.close()
        sevenZFile = try {
            SevenZFile(file.tempFile.toFile())
        } catch (ex: IOException) {
            throw ConversionFailedException("Cannot open 7zip file", ex)
        }
    }

    override fun files(
        entryFilter: (ArchiveEntry) -> Boolean,
    ): Sequence<Pair<ArchiveEntry, InputStream>> =
        generateSequence { sevenZFile.nextEntry }
            .filter { entry -> !entry.isDirectory && entryFilter(entry) }
            .map { entry ->
                val inputStream = try {
                    sevenZFile.getInputStream(entry)
                } catch (ex: IOException) {
                    throw ConversionFailedException("Cannot read 7zip file", ex)
                }
                entry to inputStream
            }

    override fun close() {
        file.use {
            sevenZFile.close()
        }
    }

    companion object {
        val sevenZipIteratorFactory: ArchiveIteratorFactory = { input, tempDir ->
            SevenZipInputStreamIterator(input, tempDir)
        }
    }
}
