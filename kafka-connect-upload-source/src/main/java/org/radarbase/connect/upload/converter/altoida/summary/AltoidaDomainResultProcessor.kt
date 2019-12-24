package org.radarbase.connect.upload.converter.altoida.summary

import org.apache.avro.generic.IndexedRecord
import org.radarbase.connect.upload.converter.LineToRecordMapper
import org.radarcns.connector.upload.altoida.AltoidaDomainResult

class AltoidaDomainResultProcessor : LineToRecordMapper {
    override val topic: String = "connect_upload_altoida_domain_result"
    override fun processLine(line: Map<String, String>, timeReceived: Double): Pair<String, IndexedRecord>? {
        return topic to AltoidaDomainResult(
                time(line),
                timeReceived,
                line.getValue("DOMAINPERCENTILE_PERCEPTUALMOTORCOORDINATION").toFloat(),
                line.getValue("DOMAINPERCENTILE_COMPLEXATTENTION").toFloat(),
                line.getValue("DOMAINPERCENTILE_COGNITIVEPROCESSINGSPEED").toFloat(),
                line.getValue("DOMAINPERCENTILE_INHIBITION").toFloat(),
                line.getValue("DOMAINPERCENTILE_FLEXIBILITY").toFloat(),
                line.getValue("DOMAINPERCENTILE_VISUALPERCEPTION").toFloat(),
                line.getValue("DOMAINPERCENTILE_PLANNING").toFloat(),
                line.getValue("DOMAINPERCENTILE_PROSPECTIVEMEMORY").toFloat(),
                line.getValue("DOMAINPERCENTILE_SPATIALMEMORY").toFloat()
        )
    }
}
