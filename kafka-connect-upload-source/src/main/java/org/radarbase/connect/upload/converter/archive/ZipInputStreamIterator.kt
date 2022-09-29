package org.radarbase.connect.upload.converter.archive

import org.apache.commons.compress.archivers.ArchiveEntry
import org.radarbase.connect.upload.exception.ConversionFailedException
import org.radarbase.connect.upload.io.TempFile
import org.radarbase.connect.upload.io.TempFile.Companion.copyToTempFile
import java.io.InputStream
import java.nio.file.Path
import java.util.zip.ZipException
import java.util.zip.ZipFile

class ZipInputStreamIterator(
    input: InputStream,
    tempDir: Path,
): ArchiveIterator {
    private val tempFile: TempFile
    private val zipFile: ZipFile

    init {
        tempFile = input.copyToTempFile(tempDir, "zip")
        input.close()
        zipFile = try {
            ZipFile(tempFile.tempFile.toFile())
        } catch (ex: ZipException) {
            throw ConversionFailedException("Cannot open Zip file", ex)
        }
    }

    override fun files(
        entryFilter: (ArchiveEntry) -> Boolean,
    ): Sequence<Pair<ArchiveEntry, InputStream>> =
        zipFile.entries()
            .asSequence()
            .map { ZipArchiveEntry(it) }
            .filter { entry -> !entry.isDirectory && entryFilter(entry) }
            .map { entry ->
                val inputStream = try {
                    zipFile.getInputStream(entry.entry)
                } catch (ex: ZipException) {
                    throw ConversionFailedException("Cannot read Zip file", ex)
                }
                entry to inputStream
            }

    override fun close() {
        tempFile.use {
            zipFile.close()
        }
    }

    companion object {
        val zipIteratorFactory: ArchiveIteratorFactory = { input, tempDir ->
            ZipInputStreamIterator(input, tempDir)
        }
    }
}
