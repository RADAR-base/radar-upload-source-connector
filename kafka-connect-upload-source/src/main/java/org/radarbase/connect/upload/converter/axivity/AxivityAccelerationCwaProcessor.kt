package org.radarbase.connect.upload.converter.axivity

import org.apache.avro.generic.IndexedRecord
import org.radarbase.connect.upload.converter.altoida.AltoidaSummaryLineToRecordMapper
import org.radarcns.connector.upload.altoida.AltoidaDomainResult
import org.radarcns.connector.upload.axivity.AxivityAcceleration

class AxivityAccelerationCwaProcessor : AxivityCwaLineToRecordMapper {
    override val topic: String = "connect_upload_axivity_acceleration"
    override fun processLine(line: Map<String, String>, timeReceived: Double): Pair<String, IndexedRecord>? {
        return topic to AxivityAcceleration(
                time(line),
                timeReceived,
                line.getValue("X").toFloat(),
                line.getValue("Y").toFloat(),
                line.getValue("Z").toFloat()
        )
    }
}
