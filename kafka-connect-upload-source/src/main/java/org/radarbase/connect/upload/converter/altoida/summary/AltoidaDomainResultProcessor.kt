package org.radarbase.connect.upload.converter.altoida.summary

import org.radarbase.connect.upload.converter.StatelessCsvLineProcessor
import org.radarbase.connect.upload.converter.TimeFieldParser
import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.altoida.summary.AltoidaSummaryProcessor.Companion.defaultTimeFormatter
import org.radarcns.connector.upload.altoida.AltoidaDomainResult

class AltoidaDomainResultProcessor : StatelessCsvLineProcessor() {
    override val fileNameSuffix: String = "export.csv"

    override val timeFieldParser: TimeFieldParser = defaultTimeFormatter

    override val header: List<String> = listOf(
        "TIMESTAMP",
        "DOMAINPERCENTILE_PERCEPTUALMOTORCOORDINATION",
        "DOMAINPERCENTILE_COMPLEXATTENTION",
        "DOMAINPERCENTILE_COGNITIVEPROCESSINGSPEED",
        "DOMAINPERCENTILE_INHIBITION",
        "DOMAINPERCENTILE_FLEXIBILITY",
        "DOMAINPERCENTILE_VISUALPERCEPTION",
        "DOMAINPERCENTILE_PLANNING",
        "DOMAINPERCENTILE_PROSPECTIVEMEMORY",
        "DOMAINPERCENTILE_SPATIALMEMORY",
    )

    override fun lineConversion(line: Map<String, String>, timeReceived: Double) =
        TopicData(
            "connect_upload_altoida_domain_result",
            AltoidaDomainResult(
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
                line.getValue("DOMAINPERCENTILE_SPATIALMEMORY").toFloat(),
                null,
                null,
            ),
        )
}
