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
import org.radarbase.connect.upload.exception.InvalidFormatException
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream


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

        /**
         * Sample cwa file content with options 14 (with temp, battery, events)
         * looks like below.
         * yyyy-MM-dd HH:mm:ss.SSS,X,Y,Z,temp,batt,events (format of the data). Not part of the actual data.
         * 2014-05-07 16:29:29.995,-0.140625,0.90625,-0.546875,279,204,r
         * 2014-05-07 16:29:30.005,0.046875,-0.296875,-1.0625,279,204,r
         * 2014-05-07 16:29:30.015,0.0625,-0.234375,-1.046875,279,204,r
         * 2014-05-07 16:29:30.025,0.046875,-0.140625,-0.984375,279,204,r
         * 2014-05-07 16:29:30.035,0.03125,-0.078125,-0.984375,279,204,r
         * 2014-05-07 16:29:30.045,0.0625,-0.046875,-1.0625,279,204,r
         * 2014-05-07 16:29:30.055,0.125,-0.078125,-1.25,279,204,r
         * 2014-05-07 16:29:30.065,0.171875,-0.1875,-1.421875,279,204,r
         *
         */
        private fun convertLines(
                contents: ContentsDTO,
                inputStream: InputStream,
                timeReceived: Double): List<FileProcessorFactory.TopicData> = readCwa(inputStream) { reader ->
            val header = CWA_HEADER
            logger.info("Header ", header)
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

        private fun <T> readCwa(
                inputStream: InputStream,
                action: (reader: CSVReader) -> T
        ): T = CSVReaderBuilder(CwaCsvInputStream(inputStream, 0, 1, -1, 14).bufferedReader())
                .withCSVParser(CSVParserBuilder().withSeparator(',').build())
                .build()
                .use { reader -> action(reader) }

    }

    companion object {
        private val logger = LoggerFactory.getLogger(CwaProcessor::class.java)
        val CWA_HEADER = listOf("TIMESTAMP", "X", "Y", "Z", "TEMP", "BATT", "EVENTS")
    }
}
