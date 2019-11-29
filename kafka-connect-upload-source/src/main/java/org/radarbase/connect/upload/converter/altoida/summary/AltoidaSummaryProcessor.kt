package org.radarbase.connect.upload.converter.altoida.summary

import org.apache.avro.generic.IndexedRecord
import org.radarbase.connect.upload.converter.altoida.AltoidaSummaryLineToRecordMapper
import org.radarcns.connector.upload.altoida.*

class AltoidaSummaryProcessor : AltoidaSummaryLineToRecordMapper {
    override val topic: String = "altoida_trial_summary"
    override fun processLine(line: Map<String, String>, timeReceived: Double): Pair<String, IndexedRecord>? {
        return topic to AltoidaSummary(
                time(line),
                timeReceived,
                line["LABEL"],
                line.getValue("AGE").toInt(),
                line.getValue("YEARSOFEDUCATION").toInt(),
                line.getValue("GENDER").toInt(),
                line.getValue("CLASS").toInt(),
                line.getValue("NMI").toDouble(),
                line.getValue("GROUNDTRUTH").toInt()
        )
    }
}
