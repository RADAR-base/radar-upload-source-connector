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
    val processorFactories: List<FileProcessorFactory>,
    val tempDir: Path,
    val generateTempFilePrefix: (ConverterFactory.ContentsContext) -> String,
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

        val preProcessedStream = inputStream.preProcessed(processors, context)

        val processStream: FileProcessorFactory.FileProcessor.(InputStream) -> Unit = { stream ->
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
        processors: List<FileProcessorFactory.FileProcessor>,
        context: ConverterFactory.ContentsContext,
    ): InputStream = processors.fold(this) { stream, processor -> processor.preProcessFile(context, stream) }

    private fun createProcessors(
        context: ConverterFactory.ContentsContext,
    ): List<FileProcessorFactory.FileProcessor> {
        val processors = processorFactories.mapNotNull { factory ->
            if (factory.matches(context.contents)) {
                factory.createProcessor(context.record)
            } else {
                null
            }
        }
        if (processors.isEmpty()) {
            throw DataProcessorNotFoundException("Cannot find data processor for record ${context.id} with file ${context.fileName}")
        }
        return processors
    }
}
