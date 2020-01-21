package org.radarbase.connect.upload.converter.altoida.summary

import org.radarbase.connect.upload.converter.StatelessCsvLineProcessor
import org.radarbase.connect.upload.converter.TimeFieldParser.DateFormatParser.Companion.formatTimeFieldParser
import org.radarbase.connect.upload.converter.TopicData
import org.radarcns.connector.upload.altoida.AltoidaSummary
import org.radarcns.connector.upload.altoida.Classification
import org.radarcns.connector.upload.altoida.GenderType
import org.radarcns.connector.upload.altoida.GroundTruth

class AltoidaSummaryProcessor : StatelessCsvLineProcessor() {
    override val fileNameSuffix: String = "export.csv"

    override val timeFieldParser = defaultTimeFormatter

    override val header: List<String> = listOf(
            "TIMESTAMP",
            "LABEL",
            "AGE",
            "YEARSOFEDUCATION",
            "GENDER",
            "CLASS",
            "NMI",
            "GROUNDTRUTH")

    override fun lineConversion(line: Map<String, String>, timeReceived: Double) =
            TopicData("altoida_trial_summary", AltoidaSummary(
                    timeFieldParser.time(line),
                    timeReceived,
                    line["LABEL"],
                    line.getValue("AGE").toInt(),
                    line.getValue("YEARSOFEDUCATION").toInt(),
                    line.getValue("GENDER").toInt().toGender(),
                    line.getValue("CLASS").toInt().classify(),
                    line.getValue("NMI").toDouble(),
                    line.getValue("GROUNDTRUTH").toInt().toGroundTruth())
            )

    private fun Int.toGender() : GenderType {
        return when (this) {
            0 -> GenderType.MALE
            1 -> GenderType.FEMALE
            2 -> GenderType.OTHER
            else -> GenderType.UNKNOWN
        }
    }

    private fun Int.classify() : Classification? {
        return when (this) {
            0 -> Classification.HEALTHY
            1 -> Classification.AT_RISK
            2 -> Classification.MCI_DUE_TO_AD
            else -> null
        }
    }

    private fun Int.toGroundTruth() : GroundTruth {
        return when (this) {
            -1 -> GroundTruth.UNKNOWN
            0 -> GroundTruth.HEALTHY
            1 -> GroundTruth.AT_RISK
            2 -> GroundTruth.MCI_OR_AB_PLUS
            3 -> GroundTruth.MCI_OR_AB_MINUS
            else -> GroundTruth.UNKNOWN
        }
    }

    companion object {
        private const val defaultTimeFormat = "yyyy-MM-dd HH:mm:ss"
        val defaultTimeFormatter = defaultTimeFormat.formatTimeFieldParser()
    }
}
