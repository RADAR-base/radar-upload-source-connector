package org.radarbase.connect.upload.converter.axivity

import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder
import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.CsvLineProcessorFactory
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarbase.connect.upload.converter.LogRepository
import org.radarbase.connect.upload.converter.axivity.newcastle.CwaCsvInputStream
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.util.*

class CwaFileProcessorFactory (
        private val processorFactories: List<CsvLineProcessorFactory>,
        private val logRepository: LogRepository
): FileProcessorFactory {

    override fun matches(contents: ContentsDTO) = processorFactories.any { it.matches(contents) }

    override fun createProcessor(record: RecordDTO) = CwaProcessor(record)

    inner class CwaProcessor(private val record: RecordDTO): FileProcessorFactory.FileProcessor {
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

        private fun convertLines(
                contents: ContentsDTO,
                inputStream: InputStream,
                timeReceived: Double): List<FileProcessorFactory.TopicData> = readCwa(inputStream) { reader ->
            val header = reader.readNext().map { it.trim().toUpperCase(Locale.US) }
            logger.info("Header ", header)
//            val processorFactory = processorFactories
//                    .find { it.matches(contents) && it.matches(header) }
//                    ?: throw InvalidFormatException("In record ${record.id}, cannot find CSV processor that matches header $header")
//
//            val processor = processorFactory.createLineProcessor(record, logRepository)
//
//            generateSequence { reader.readNext() }
//                    .filter { processor.isLineValid(header, it) }
//                    .mapNotNull { processor.convertToRecord(header.zip(it).toMap(), timeReceived) }
//                    .toList()
//                    .flatten()
            return@readCwa emptyList<FileProcessorFactory.TopicData>()
        }

        private fun <T> readCwa(
                inputStream: InputStream,
                action: (reader: CSVReader) -> T
        ): T = CSVReaderBuilder(CwaCsvInputStream(inputStream).bufferedReader())
                .withCSVParser(CSVParserBuilder().withSeparator(',').build())
                .build()
                .use { reader -> action(reader) }

    }

    companion object {
        private val logger = LoggerFactory.getLogger(CwaProcessor::class.java)
    }
}
