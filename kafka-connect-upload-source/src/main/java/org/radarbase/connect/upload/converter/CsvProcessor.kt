package org.radarbase.connect.upload.converter

import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder
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
        private val headerProcessorFactories: List<CsvHeaderProcessorFactory>?,
        private val processorFactories: List<CsvLineProcessorFactory>): FileProcessorFactory.FileProcessor {
    private val recordLogger = logRepository.createLogger(logger, record.id!!)

    override fun processData(contents: ContentsDTO, inputStream: InputStream, timeReceived: Double): List<TopicData> {
        return try {
            convertLines(contents, inputStream, timeReceived)
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
            timeReceived: Double): List<TopicData> {
        val contentProcessors = processorFactories
                .filter { it.matches(contents) }

        if (contents.size == 0L) {
            if (contentProcessors.all { it.optional }) {
                logger.debug("Skipping optional file")
                return emptyList()
            } else {
                throw IOException("Cannot read empty CSV file ${contents.fileName}")
            }
        }

        val headerProcessors = headerProcessorFactories?.filter { it.matches(contents)}

        return inputStream.bufferedReader().toCsvReader().use { reader ->
            var header = reader.readNext()?.map { it.trim().toUpperCase(Locale.US) }
                    ?: if (contentProcessors.all { it.optional }) {
                        logger.debug("Skipping optional file")
                        return@use emptyList()
                    } else {
                        throw IOException("Cannot read empty CSV file ${contents.fileName}")
                    }

            headerProcessors?.map {
                header = it.process(header)
            }

            val processors = contentProcessors
                    .filter {
                        when {
                            it.matches(header) -> true
                            it.headerMustMatch -> throw InvalidFormatException(
                                """
                                    CSV header of file ${contents.fileName} in record ${record.id} did not match processor ${it.javaClass}
                                        Found:    $header
                                        Expected: ${it.header}
                                """.trimIndent()
                            )
                            else -> false
                        }
                    }
                    .map { it.createLineProcessor(record, logRepository) }
                    .takeIf { it.isNotEmpty() }
                    ?: throw InvalidFormatException("For file ${contents.fileName} in record ${record.id}, cannot find CSV processor that matches header $header")

            generateSequence { reader.readNext() }
                    .map { line ->
                        val lineMap = header.zip(line).toMap()
                        processors.flatMap { processor ->
                            processor.takeIf { it.isLineValid(header, line) }
                                    ?.convertToRecord(lineMap, timeReceived)
                                    ?: emptyList()
                        }
                    }
                    .toList()
                    .flatten()
        }
    }

    protected open fun BufferedReader.toCsvReader(): CSVReader = CSVReaderBuilder(this)
            .withCSVParser(CSVParserBuilder().withSeparator(',').build())
            .build()

    companion object {
        private val logger = LoggerFactory.getLogger(CsvProcessor::class.java)
    }
}

