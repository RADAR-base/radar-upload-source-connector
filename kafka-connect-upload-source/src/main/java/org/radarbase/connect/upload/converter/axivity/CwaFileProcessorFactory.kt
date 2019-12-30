package org.radarbase.connect.upload.converter.axivity

import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.api.SourceTypeDTO
import org.radarbase.connect.upload.converter.*
import org.radarbase.connect.upload.converter.axivity.newcastle.CwaCsvInputStream
import org.radarbase.connect.upload.exception.InvalidFormatException
import org.slf4j.LoggerFactory
import java.io.InputStream

class CwaFileProcessorFactory(
        override val processorFactories: List<CsvLineProcessorFactory>,
        override val logRepository: LogRepository,
        private val configuration: SourceTypeDTO
) : CsvFileProcessorFactory(processorFactories, logRepository) {

    override fun createProcessor(record: RecordDTO) = CwaProcessor(record)

    inner class CwaProcessor(private val record: RecordDTO) : CsvProcessor(record, logRepository, processorFactories) {

        override fun convertLines(
                contents: ContentsDTO,
                inputStream: InputStream,
                timeReceived: Double): List<FileProcessorFactory.TopicData> {
            checkNotNull(configuration.configuration)
            return readCsv(CwaCsvInputStream(inputStream, 0, 1, -1, cwaReadOptions).bufferedReader()) { reader ->
                val header = CWA_HEADER
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


        val cwaReadOptions: Int
            get() {
                val readAlso = configuration.configuration?.getOrDefault("readAlso", null) ?: return 0
                logger.info("Reading $readAlso from a cwa file.")
                val readValues = readAlso.split(" ")
                val readOption = 0
                readValues.forEach {
                    when (it) {
                        "--temp" -> {
                            readOption.or(CwaCsvInputStream.OPTIONS_TEMP)
                            CWA_HEADER.add("TEMPERATURE")
                        }
                        "--light" -> {
                            readOption.or(CwaCsvInputStream.OPTIONS_LIGHT)
                            CWA_HEADER.add("LIGHT")
                        }
                        "--battery" -> {
                            readOption.or(CwaCsvInputStream.OPTIONS_BATT)
                            CWA_HEADER.add("BATTERY")
                        }
                        "--events" -> {
                            readOption.or(CwaCsvInputStream.OPTIONS_EVENTS)
                            CWA_HEADER.add("EVENTS")
                        }
                    }
                }
                logger.info("Setting CWA read option to $readOption and headers to $CWA_HEADER")
                return readOption
            }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CwaProcessor::class.java)
        val CWA_HEADER = mutableListOf("TIMESTAMP", "X", "Y", "Z")
    }
}
