package org.radarbase.connect.upload.converter

import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder
import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.exception.InvalidFormatException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.util.*

/**
 * Base class to process CSV files with multiple possible per-CSV-line processors.
 */
open class CsvProcessor(
    private val record: RecordDTO,
    private val processorFactories: List<CsvLineProcessorFactory>,
): FileProcessorFactory.FileProcessor {

    override fun processData(
        context: ConverterFactory.ContentsContext,
        inputStream: InputStream,
        produce: (TopicData) -> Unit,
    ) {
        val contentProcessorsFactories = processorFactories
            .filter { it.matches(context.contents) }

        if (context.contents.size == 0L) {
            checkCsvEmpty(contentProcessorsFactories, context)
            return
        }

        inputStream.bufferedReader().toCsvReader().use { reader ->
            processCsv(reader, contentProcessorsFactories, context)
                .forEach(produce)
        }
    }

    /**
     * Check whether having an empty CSV file is allowed.
     * @throws IOException if the CSV file should have contents.
     */
    private fun checkCsvEmpty(
        contentProcessorsFactories: List<CsvLineProcessorFactory>,
        context: ConverterFactory.ContentsContext,
    ) {
        if (contentProcessorsFactories.all { it.optional }) {
            context.logger.debug("Skipping optional file ${context.fileName}")
        } else {
            throw IOException("Cannot read empty CSV file ${context.fileName}")
        }
    }

    private fun processCsv(
        reader: CSVReader,
        contentProcessorsFactories: List<CsvLineProcessorFactory>,
        context: ConverterFactory.ContentsContext,
    ): Sequence<TopicData> {
        val rawHeader = reader.readNext()
        if (rawHeader == null) {
            checkCsvEmpty(contentProcessorsFactories, context)
            return emptySequence()
        }
        val header = rawHeader.map { it.trim().toUpperCase(Locale.US) }

        val processors = contentProcessorsFactories.createLineProcessors(context, header)

        return generateSequence { reader.readNext() }
            .flatMapIndexed { idx: Int, line: Array<String> ->
                val lineMap = header.zip(line).toMap()
                val lineNumber = idx + 2

                processors
                    .asSequence()
                    .filter { it.isLineValid(header, line, lineNumber) }
                    .flatMap { it.convertToRecord(lineMap, context.timeReceived) }
            }
    }

    protected open fun CsvLineProcessorFactory.checkHeader(
        contents: ContentsDTO,
        header: List<String>,
    ): Boolean = when {
        matches(header) -> true
        headerMustMatch -> throw InvalidFormatException(
            """
            CSV header of file ${contents.fileName} in record ${record.id} did not match processor $javaClass
                Found:    $header
                Expected: ${this.header}
            """.trimIndent()
        )
        else -> false
    }

    protected open fun BufferedReader.toCsvReader(): CSVReader = CSVReaderBuilder(this)
            .withCSVParser(CSVParserBuilder().withSeparator(',').build())
            .build()

    private fun List<CsvLineProcessorFactory>.createLineProcessors(
        context: ConverterFactory.ContentsContext,
        header: List<String>,
    ): List<CsvLineProcessorFactory.CsvLineProcessor> {
        val processors = this
            .filter { it.checkHeader(context.contents, header) }
            .map { it.createLineProcessor(context) }

        if (processors.isEmpty()) {
            throw InvalidFormatException("For file ${context.fileName} in record ${context.id}, cannot find CSV processor that matches header $header")
        }

        return processors
    }
}
