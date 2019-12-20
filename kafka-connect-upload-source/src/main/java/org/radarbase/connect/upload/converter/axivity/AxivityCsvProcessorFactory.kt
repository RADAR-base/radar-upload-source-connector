package org.radarbase.connect.upload.converter.axivity;

import org.apache.avro.generic.IndexedRecord
import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.CsvLineProcessorFactory
import org.radarbase.connect.upload.converter.LogRepository
import org.radarbase.connect.upload.converter.MultiRecordsCsvLineProcessor
import org.radarbase.connect.upload.converter.axivity.CwaFileProcessorFactory.Companion.CWA_HEADER
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


interface AxivityCwaLineToRecordMapper {
    val topic: String
    fun time(line: Map<String, String>): Double =
            Instant.from(
                    DateTimeFormatter
                            .ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                            .withZone(ZoneId.systemDefault())
                            .parse(line.getValue("TIMESTAMP")))
                    .toEpochMilli()
                    .toDouble()
    fun processLine(line: Map<String, String>, timeReceived: Double): Pair<String, IndexedRecord>?
}

abstract class AxivityCsvMultiRecordProcessorFactory : CsvLineProcessorFactory {
    private val logger = LoggerFactory.getLogger(javaClass)

    abstract val fileNameSuffix: String

    abstract val listOfRecordMapperAltoidaSummaries: List<AxivityCwaLineToRecordMapper>

    fun lineConversion(line: Map<String, String>, timeReceived: Double): List<Pair<String, IndexedRecord>> =
            listOfRecordMapperAltoidaSummaries.mapNotNull { it.processLine(line, timeReceived) }

    override fun matches(contents: ContentsDTO): Boolean = contents.fileName.endsWith(fileNameSuffix)

    override fun createLineProcessor(record: RecordDTO, logRepository: LogRepository): CsvLineProcessorFactory.CsvLineProcessor {
        val recordLogger = logRepository.createLogger(logger, record.id!!)
        return MultiRecordsCsvLineProcessor(recordLogger) { l, t -> lineConversion(l, t) }
    }

}

class AxivityCsvProcessorFactory() : AxivityCsvMultiRecordProcessorFactory() {
    override val fileNameSuffix: String = ".cwa"
    override val header: List<String> = CWA_HEADER
    override val listOfRecordMapperAltoidaSummaries: List<AxivityCwaLineToRecordMapper>
        get() = listOf(
            AxivityAccelerationCwaProcessor()
        )
}
