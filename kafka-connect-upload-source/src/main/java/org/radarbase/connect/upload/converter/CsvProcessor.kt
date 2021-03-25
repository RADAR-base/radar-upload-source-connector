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

open class CsvProcessor(
    private val record: RecordDTO,
    private val processorFactories: List<CsvLineProcessorFactory>,
): FileProcessorFactory.FileProcessor {

    override fun processData(
        context: ConverterFactory.ContentsContext,
        inputStream: InputStream,
        produce: (TopicData) -> Unit,
    ) {
        val contentProcessors = processorFactories
            .filter { it.matches(context.contents) }

        if (context.contents.size == 0L) {
            if (contentProcessors.all { it.optional }) {
                context.logger.debug("Skipping optional file")
                return
            } else {
                throw IOException("Cannot read empty CSV file ${context.fileName}")
            }
        }

        inputStream.bufferedReader().toCsvReader().use { reader ->
            val header = reader.readNext()?.map { it.trim().toUpperCase(Locale.US) }
                ?: if (contentProcessors.all { it.optional }) {
                    context.logger.debug("Skipping optional file")
                    return@use
                } else {
                    throw IOException("Cannot read empty CSV file ${context.fileName}")
                }

            val processors = contentProcessors
                .filter { it.checkHeader(context.contents, header) }
                .map { it.createLineProcessor(context) }
                .takeIf { it.isNotEmpty() }
                ?: throw InvalidFormatException("For file ${context.fileName} in record ${context.id}, cannot find CSV processor that matches header $header")


            generateSequence { reader.readNext() }
                .flatMap { line: Array<String> ->
                    processors
                        .asSequence()
                        .filter { it.isLineValid(header, line) }
                        .map { Pair(it, header.zip(line).toMap()) }
                }
                .flatMap { (processor, lineMap) ->
                    processor.convertToRecord(lineMap, context.timeReceived)
                }
                .forEach { produce(it) }
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
}

