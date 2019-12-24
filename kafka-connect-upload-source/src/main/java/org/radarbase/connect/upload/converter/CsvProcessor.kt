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

    override fun processData(contents: ContentsDTO, inputStream: InputStream, timeReceived: Double): List<FileProcessorFactory.TopicData> {
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
            timeReceived: Double): List<FileProcessorFactory.TopicData> = readCsv(inputStream.bufferedReader()) { reader ->
        val header = reader.readNext().map { it.trim().toUpperCase(Locale.US) }
        val processorFactory = processorFactories
                .find { it.matches(contents) && it.matches(header) }
                ?: throw InvalidFormatException("In record ${record.id}, cannot find CSV processor that matches header $header")

        val processor = processorFactory.createLineProcessor(record, logRepository)

        generateSequence { reader.readNext() }
                .filter { processor.isLineValid(header, it) }
                .mapNotNull { processor.convertToRecord(header.zip(it).toMap(), timeReceived) }
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

