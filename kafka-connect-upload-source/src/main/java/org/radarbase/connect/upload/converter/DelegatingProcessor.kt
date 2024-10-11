package org.radarbase.connect.upload.converter

import org.radarbase.connect.upload.exception.DataProcessorNotFoundException
import org.radarbase.connect.upload.io.TempFile.Companion.copyToTempFile
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

/**
 * Delegate file processing to multiple underlying processors.
 */
class DelegatingProcessor(
    private val preProcessorFactories: List<FilePreProcessorFactory>,
    val processorFactories: List<FileProcessorFactory>,
    val tempDir: Path,
    val generateTempFilePrefix: (ConverterFactory.ContentsContext) -> String,
    val allowUnmappedFiles: Boolean = false,
) {
    init {
        Files.createDirectories(tempDir)
    }

    fun processData(
        context: ConverterFactory.ContentsContext,
        inputStream: InputStream,
        produce: (TopicData) -> Unit,
    ) {
        context.logger.debug("Processing entry ${context.fileName} from record ${context.id}")

        val processors = createProcessors(context)

        if (processors.isEmpty()) {
            if (!allowUnmappedFiles) {
                throw DataProcessorNotFoundException("Cannot find data processor for record ${context.id} with file ${context.fileName}")
            } else {
                context.logger.info("Skipping unmapped file ${context.fileName}..")
                return
            }
        }

        val preProcessedStream = inputStream.preProcessed(createPreProcessors(context), context)

        val processStream: FileProcessor.(InputStream) -> Unit = { stream ->
            context.logger.debug("Processing ${context.fileName} with ${javaClass.simpleName} processor")
            processData(context, stream, produce)
        }

        if (processors.size == 1) {
            processors.first().processStream(preProcessedStream)
        } else {
            preProcessedStream.copyToTempFile(
                tempDir,
                generateTempFilePrefix(context),
            ).use { tempFile ->
                processors.forEach { it.processStream(tempFile.inputStream()) }
            }
        }
    }

    private fun InputStream.preProcessed(
        processors: List<FilePreProcessor>,
        context: ConverterFactory.ContentsContext,
    ): InputStream = processors.fold(this) { stream, processor -> processor.preProcessFile(context, stream) }

    private fun createPreProcessors(
        context: ConverterFactory.ContentsContext,
    ): List<FilePreProcessor> {
        return preProcessorFactories.mapNotNull { factory ->
            if (factory.matches(context.contents)) {
                factory.createPreProcessor(context.record)
            } else {
                null
            }
        }
    }

    private fun createProcessors(
        context: ConverterFactory.ContentsContext,
    ): List<FileProcessor> {
        return processorFactories.mapNotNull { factory ->
            if (factory.matches(context.contents)) {
                factory.createProcessor(context.record)
            } else {
                null
            }
        }
    }
}
