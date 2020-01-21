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
            timeReceived: Double): List<TopicData> = readCsv(inputStream.bufferedReader()) { reader ->
        val header = reader.readNext().map { it.trim().toUpperCase(Locale.US) }
        val processors = processorFactories
                .filter { it.matches(contents) && it.matches(header) }
                .map { it.createLineProcessor(record, logRepository) }
                .takeIf { it.isNotEmpty() }
                ?: throw InvalidFormatException("In record ${record.id}, cannot find CSV processor that matches header $header")

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

    fun <T> readCsv(
            inputReader: BufferedReader,
            action: (reader: CSVReader) -> T
    ): T = CSVReaderBuilder(inputReader)
            .withCSVParser(CSVParserBuilder().withSeparator(',').build())
            .build()
            .use { reader -> action(reader) }

    companion object {
        private val logger = LoggerFactory.getLogger(CsvProcessor::class.java)
    }
}

