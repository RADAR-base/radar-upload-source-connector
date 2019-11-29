package org.radarbase.connect.upload.converter.altoida

import org.apache.avro.generic.IndexedRecord
import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.CsvLineProcessorFactory
import org.radarbase.connect.upload.converter.LogRepository
import org.radarbase.connect.upload.converter.MultiRecordsCsvLineProcessor
import org.slf4j.LoggerFactory

abstract class AltoidaCsvMultiRecordsProcessor: CsvLineProcessorFactory {
    private val logger = LoggerFactory.getLogger(javaClass)

    abstract val fileNameSuffix: String

    abstract fun MultiRecordsCsvLineProcessor.lineConversion(line: Map<String, String>, timeReceived: Double): Map<String,IndexedRecord>?

    override fun matches(contents: ContentsDTO): Boolean = contents.fileName.endsWith(fileNameSuffix)

    open fun time(line: Map<String, String>): Double = line.getValue("TIMESTAMP").toDouble() / 1000.0

    override fun createLineProcessor(record: RecordDTO, logRepository: LogRepository): CsvLineProcessorFactory.CsvLineProcessor {
        val recordLogger = logRepository.createLogger(logger, record.id!!)
        return MultiRecordsCsvLineProcessor(recordLogger) { l, t -> lineConversion(l, t) }
    }

}
