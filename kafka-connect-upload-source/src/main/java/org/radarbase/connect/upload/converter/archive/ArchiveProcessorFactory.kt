package org.radarbase.connect.upload.converter.archive

import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.*
import org.radarbase.connect.upload.converter.archive.SevenZipInputStreamIterator.Companion.sevenZipIteratorFactory
import org.radarbase.connect.upload.converter.archive.ZipInputStreamIterator.Companion.zipIteratorFactory
import org.radarbase.connect.upload.exception.ConversionFailedException
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths

typealias ArchiveIteratorFactory = (InputStream, Path) -> ArchiveIterator

open class ArchiveProcessorFactory(
    sourceType: String,
    entryPreProcessors: List<FilePreProcessorFactory> = emptyList(),
    entryProcessors: List<FileProcessorFactory>,
    allowUnmappedFiles: Boolean = false,
    private val extension: String,
    private val archiveIteratorFactory: ArchiveIteratorFactory
): FileProcessorFactory {
    private val cacheDir: Path = Paths.get(
        System.getProperty("java.io.tmpdir"),
        "upload-connector",
        "$sourceType-$extension-cache"
    )

    private val delegatingProcessor = DelegatingProcessor(
        preProcessorFactories = entryPreProcessors,
        processorFactories = entryProcessors,
        tempDir = cacheDir,
        generateTempFilePrefix = { context ->
            val safeEntryName = context.fileName
                .replace(nonAlphaNumericRegex, "")
                .takeLast(50)
            "record-entry-${context.id}-$safeEntryName-"
        },
        allowUnmappedFiles = allowUnmappedFiles,
    )

    open fun entryFilter(name: String): Boolean = true

    override fun matches(contents: ContentsDTO): Boolean = contents.fileName.endsWith(extension, ignoreCase = true)

    open fun beforeProcessing(contents: ConverterFactory.ContentsContext) = Unit

    open fun afterProcessing(contents: ConverterFactory.ContentsContext) = Unit

    override fun createProcessor(record: RecordDTO): FileProcessor = ArchiveFileProcessor(record)

    inner class ArchiveFileProcessor(private val record: RecordDTO):
        FileProcessor {
        override fun processData(
            context: ConverterFactory.ContentsContext,
            inputStream: InputStream,
            produce: (TopicData) -> Unit
        ) {
            context.logger.info("Retrieved archive content from filename ${context.fileName}")
            beforeProcessing(context)
            try {
                archiveIteratorFactory(inputStream, cacheDir).use { iterator ->
                    iterator.files { entryFilter(it.name.trim()) }
                        .ifEmpty { throw ConversionFailedException("No archive entry found from ${context.fileName}") }
                        .forEach { (entry, inputStream) ->
                            delegatingProcessor.processData(
                                context.copy(
                                    contents = ContentsDTO(
                                        fileName = entry.name.trim(),
                                        size = entry.size
                                    ),
                                ),
                                inputStream,
                                produce,
                            )
                        }
                }
            } catch (exe: IOException) {
                context.logger.error("Failed to process archive input from record ${record.id}", exe)
                throw exe
            } catch (exe: Exception) {
                context.logger.error("Could not process record ${record.id}", exe)
                throw exe
            } finally {
                afterProcessing(context)
            }
        }
    }

    companion object {
        private val nonAlphaNumericRegex = "\\W+".toRegex()

        fun zipFactory(
            sourceType: String,
            entryPreProcessors: List<FilePreProcessorFactory> = emptyList(),
            entryProcessors: List<FileProcessorFactory>,
            allowUnmappedFiles: Boolean = false,
        ) = ArchiveProcessorFactory(
            sourceType = sourceType,
            entryPreProcessors = entryPreProcessors,
            entryProcessors = entryProcessors,
            allowUnmappedFiles = allowUnmappedFiles,
            extension = ".zip",
            archiveIteratorFactory = zipIteratorFactory,
        )

        fun sevenZipFactory(
            sourceType: String,
            entryPreProcessors: List<FilePreProcessorFactory> = emptyList(),
            entryProcessors: List<FileProcessorFactory>,
            allowUnmappedFiles: Boolean = false,
        ) = ArchiveProcessorFactory(
            sourceType = sourceType,
            entryPreProcessors = entryPreProcessors,
            entryProcessors = entryProcessors,
            allowUnmappedFiles = allowUnmappedFiles,
            extension = ".7z",
            archiveIteratorFactory = sevenZipIteratorFactory,
        )
    }
}
