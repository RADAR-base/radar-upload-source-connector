package org.radarbase.connect.upload.converter.altoida

import org.apache.avro.generic.IndexedRecord
import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.CsvLineProcessorFactory
import org.radarbase.connect.upload.converter.LogRepository
import org.radarbase.connect.upload.converter.MultiRecordsCsvLineProcessor
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


interface AltoidaSummaryLineToRecordMapper {
    val topic: String
    fun time(line: Map<String, String>): Double =
            Instant.from(
                    DateTimeFormatter
                            .ofPattern("yyyy-MM-dd HH:mm:ss")
                            .withZone(ZoneId.systemDefault())
                            .parse(line.getValue("TIMESTAMP")))
                            .toEpochMilli()
                            .toDouble()
    fun processLine(line: Map<String, String>, timeReceived: Double): Pair<String, IndexedRecord>?
}

abstract class AltoidaCsvMultiRecordsProcessor : CsvLineProcessorFactory {
    private val logger = LoggerFactory.getLogger(javaClass)

    abstract val fileNameSuffix: String

    abstract val listOfRecordMapperAltoidaSummaries: List<AltoidaSummaryLineToRecordMapper>

    fun lineConversion(line: Map<String, String>, timeReceived: Double): List<Pair<String, IndexedRecord>> =
            listOfRecordMapperAltoidaSummaries.mapNotNull { it.processLine(line, timeReceived) }

    override fun matches(contents: ContentsDTO): Boolean = contents.fileName.endsWith(fileNameSuffix)

    override fun createLineProcessor(record: RecordDTO, logRepository: LogRepository): CsvLineProcessorFactory.CsvLineProcessor {
        val recordLogger = logRepository.createLogger(logger, record.id!!)
        return MultiRecordsCsvLineProcessor(recordLogger) { l, t -> lineConversion(l, t) }
    }

}