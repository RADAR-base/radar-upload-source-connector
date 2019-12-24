package org.radarbase.connect.upload.converter

import org.apache.avro.generic.IndexedRecord
import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

abstract class OneToManyCsvLineProcessorFactory : CsvLineProcessorFactory {
    private val logger = LoggerFactory.getLogger(javaClass)

    abstract val fileNameSuffix: String

    abstract val recordMappers: List<LineToRecordMapper>

    fun lineConversion(line: Map<String, String>, timeReceived: Double): List<Pair<String, IndexedRecord>> =
            recordMappers.mapNotNull { it.processLine(line, timeReceived) }

    override fun matches(contents: ContentsDTO): Boolean = contents.fileName.endsWith(fileNameSuffix)


    override fun createLineProcessor(record: RecordDTO, logRepository: LogRepository): CsvLineProcessorFactory.CsvLineProcessor {
        val recordLogger = logRepository.createLogger(logger, record.id!!)
        return OneToManyCsvLineProcessor(recordLogger) { l, t -> lineConversion(l, t) }
    }

}

/**
 * Allows to process one line to multiple records of various topics.
 */
class OneToManyCsvLineProcessor(
        override val recordLogger: RecordLogger,
        private val conversion: OneToManyCsvLineProcessor.(lineValues: Map<String, String>, timeReceived: Double) -> List<Pair<String, IndexedRecord>>
) : CsvLineProcessorFactory.CsvLineProcessor {
    override fun convertToRecord(lineValues: Map<String, String>, timeReceived: Double): List<FileProcessorFactory.TopicData>? {
        return conversion(lineValues, timeReceived).run {
            this.map { FileProcessorFactory.TopicData(it.first, it.second) }
        }
    }
}

interface LineToRecordMapper {
    val topic: String

    fun time(line: Map<String, String>, timeFormat: String): Double =
            Instant.from(
                    DateTimeFormatter
                            .ofPattern(timeFormat)
                            .withZone(ZoneId.systemDefault())
                            .parse(line.getValue("TIMESTAMP")))
                    .toEpochMilli()
                    .toDouble()
    fun time(line: Map<String, String>): Double = time(line, "yyyy-MM-dd HH:mm:ss")

    fun processLine(line: Map<String, String>, timeReceived: Double): Pair<String, IndexedRecord>?
}
