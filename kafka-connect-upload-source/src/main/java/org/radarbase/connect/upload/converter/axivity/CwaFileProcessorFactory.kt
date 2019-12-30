package org.radarbase.connect.upload.converter.axivity

import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.*
import org.radarbase.connect.upload.converter.axivity.newcastle.CwaCsvInputStream
import org.radarbase.connect.upload.exception.InvalidFormatException
import org.slf4j.LoggerFactory
import java.io.InputStream

class CwaFileProcessorFactory(
        override val processorFactories: List<CsvLineProcessorFactory>,
        override val logRepository: LogRepository,
        private val header: List<String>,
        private val readOption: Int
) : CsvFileProcessorFactory(processorFactories, logRepository) {

    override fun createProcessor(record: RecordDTO) = CwaProcessor(record)

    inner class CwaProcessor(private val record: RecordDTO) : CsvProcessor(record, logRepository, processorFactories) {

        override fun convertLines(
                contents: ContentsDTO,
                inputStream: InputStream,
                timeReceived: Double): List<FileProcessorFactory.TopicData> = readCsv(CwaCsvInputStream(inputStream, 0, 1, -1, readOption).bufferedReader()) { reader ->
            logger.info("Current Headers are $header")
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

    }

    companion object {
        private val logger = LoggerFactory.getLogger(CwaProcessor::class.java)
    }
}
