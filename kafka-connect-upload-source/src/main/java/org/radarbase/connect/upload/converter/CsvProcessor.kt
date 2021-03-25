package org.radarbase.connect.upload.converter

import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder
import kotlinx.coroutines.flow.emptyFlow
import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.exception.InvalidFormatException
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.util.*

open class CsvProcessor(
        private val record: RecordDTO,
        private val logRepository: LogRepository,
        private val processorFactories: List<CsvLineProcessorFactory>): FileProcessorFactory.FileProcessor {
    private val recordLogger = logRepository.createLogger(logger, record.id!!)

    override fun processData(
        context: ConverterFactory.ContentsContext,
        inputStream: InputStream,
        timeReceived: Double,
        produce: (TopicData) -> Unit
    ) {
        try {
            convertLines(context.contents, inputStream, timeReceived, produce)
        } catch (exe: IOException) {
            recordLogger.error("Something went wrong while processing a contents of record ${record.id}: ${exe.message} ")
            throw exe
        } finally {
            recordLogger.info("Closing resources of content")
        }
    }

    open fun convertLines(
        contents: ContentsDTO,
        inputStream: InputStream,
        timeReceived: Double,
        produce: (TopicData) -> Unit
    ) {
        val contentProcessors = processorFactories
                .filter { it.matches(contents) }

        if (contents.size == 0L) {
            if (contentProcessors.all { it.optional }) {
                logger.debug("Skipping optional file")
                return
            } else {
                throw IOException("Cannot read empty CSV file ${contents.fileName}")
            }
        }

        inputStream.bufferedReader().toCsvReader().use { reader ->
            val header = reader.readNext()?.map { it.trim().toUpperCase(Locale.US) }
                ?: if (contentProcessors.all { it.optional }) {
                    logger.debug("Skipping optional file")
                    return@use
                } else {
                    throw IOException("Cannot read empty CSV file ${contents.fileName}")
                }

            val processors = contentProcessors
                .filter { it.checkHeader(contents, header) }
                .map { it.createLineProcessor(record, logRepository) }
                .takeIf { it.isNotEmpty() }
                ?: throw InvalidFormatException("For file ${contents.fileName} in record ${record.id}, cannot find CSV processor that matches header $header")


            generateSequence { reader.readNext() }
                .flatMap { line: Array<String> ->
                    val lineMap = header.zip<String, String>(line).toMap()
                    processors
                        .asSequence()
                        .filter { it.isLineValid(header, line) }
                        .flatMap { processor ->
                            processor.convertToRecord(lineMap, timeReceived)
                                ?: emptySequence()
                        }
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

    companion object {
        private val logger = LoggerFactory.getLogger(CsvProcessor::class.java)
    }
}

