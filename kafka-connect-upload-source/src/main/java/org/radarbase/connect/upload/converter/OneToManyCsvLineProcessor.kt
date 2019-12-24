package org.radarbase.connect.upload.converter

import org.apache.avro.generic.IndexedRecord

/**
 * Allows to process one line to multiple records of various topics.
 */
class OneToManyCsvLineProcessor(
        override val recordLogger: RecordLogger,
        private val conversion: OneToManyCsvLineProcessor.(lineValues: Map<String, String>, timeReceived: Double) -> List<Pair<String,IndexedRecord>>
) : CsvLineProcessorFactory.CsvLineProcessor {
    override fun convertToRecord(lineValues: Map<String, String>, timeReceived: Double): List<FileProcessorFactory.TopicData>? {
        return conversion(lineValues, timeReceived).run {
            this.map { FileProcessorFactory.TopicData(it.first, it.second) }
        }
    }
}
