package org.radarbase.connect.upload.converter.altoida.summary

import org.apache.avro.generic.IndexedRecord
import org.radarbase.connect.upload.converter.LineToRecordMapper
import org.radarbase.connect.upload.converter.altoida.summary.AltoidaExportCsvProcessor.AltoidaTestCategory
import org.radarbase.connect.upload.converter.altoida.summary.AltoidaTestMetricsProcessor.AltoidaTappingTestTypes.RandomTapping
import org.radarbase.connect.upload.converter.altoida.summary.AltoidaTestMetricsProcessor.AltoidaTappingTestTypes.Tapping
import org.radarbase.connect.upload.converter.altoida.summary.AltoidaTestMetricsProcessor.AltoidaWalkingTestTypes.*
import org.radarcns.connector.upload.altoida.AltoidaSummaryMetrics
import org.radarcns.connector.upload.altoida.AltoidaTappingTestAggregate
import org.radarcns.connector.upload.altoida.AltoidaTrial
import org.radarcns.connector.upload.altoida.AltoidaWalkingTestAggregate

class AltoidaTestMetricsProcessor(private val type: AltoidaTestCategory, override val topic: String) : LineToRecordMapper {
    override fun processLine(line: Map<String, String>, timeReceived: Double): Pair<String, IndexedRecord>? {
        return topic to getTestMetrics(type, line)
    }

    private fun getTestMetrics(type: AltoidaTestCategory, line: Map<String, String>): IndexedRecord {
        val prefix = when (type) {
            AltoidaTestCategory.BIT -> "BIT_"
            AltoidaTestCategory.DOT -> "DOT_"
        }

        return AltoidaSummaryMetrics(
                line.getValue("${prefix}PLAYHIGHREACTIONTIMES").toFloat(),
                line.getValue("${prefix}PLAYHIGHACCURACY").toFloat(),
                line.getValue("${prefix}PLAYLOWREACTIONS").toInt(),
                line.getValue("${prefix}IGNOREDHIGHTONEPERCENTAGE").toFloat(),
                line.getValue("${prefix}PREMATURETONEBUTTONPRESSES").toInt(),
                line.getValue("${prefix}RANDOMSCREENPRESSESDURINGPLACEMENT").toInt(),
                line.getValue("${prefix}RANDOMSCREENPRESSESDURINGSEARCH").toInt(),
                line.getValue("${prefix}TOOMUCHMOVEMENTCOUNT").toInt(),
                line.getValue("${prefix}FINDBETTERPLACECOUNT").toFloat(),
                line.getValue("${prefix}INTROREADTIMES").toFloat(),
                line.getValue("${prefix}INTROREADTIME1").toFloat(),
                line.getValue("${prefix}INTROREADTIME2").toFloat(),
                line.getValue("${prefix}PLACEDELAYS").toFloat(),
                line.getValue("${prefix}SPOTALREADYTAKENCOUNT").toInt(),
                line.getTrails(prefix),
                line.getTrailMeans(prefix),
                line.getValue("${prefix}FINDFAILCOUNT").toFloat(),
                line.getValue("${prefix}FINDSKIPDURATIONS").toFloat(),
                line.getValue("${prefix}SKIPBUTTONCOUNT").toFloat(),
                line.getValue("${prefix}COUNTDOWNFAIL").toFloat(),
                line.getValue("${prefix}STEPCOUNTPFRATIO").toFloat(),
                line.getValue("${prefix}MEANSTEPDELAYP").toFloat(),
                line.getValue("${prefix}MEANSTEPDELAYF").toFloat(),
                line.getValue("${prefix}STEPVARIANCEP").toFloat(),
                line.getValue("${prefix}STEPVARIANCEF").toFloat(),
                line.getValue("${prefix}NOTWALKINGTIMEP").toFloat(),
                line.getValue("${prefix}NOTWALKINGTIMEF").toFloat(),
                line.getValue("${prefix}SHOCKCOUNT").toFloat(),
                line.getValue("${prefix}ACCVARIANCE1").toFloat(),
                line.getValue("${prefix}ACCVARIANCE2").toFloat(),
                line.getValue("${prefix}ACCVARIANCE3").toFloat(),
                line.getValue("${prefix}STRONGHAND").toFloat(),
                line.getWalkingTestAggregate(prefix, Circle),
                line.getWalkingTestAggregate(prefix, Square),
                line.getWalkingTestAggregate(prefix, Serpentine),
                line.getWalkingTestAggregate(prefix, SpeedCircle),
                line.getTappingTestAggregate(prefix, RandomTapping),
                line.getTappingTestAggregate(prefix, Tapping)
        )
    }

    private fun Map<String, String>.getTrails(prefix: String): List<AltoidaTrial> {
        return listOf("1", "2", "3").map { testIndex ->
            AltoidaTrial(
                    this.getValue("${prefix}PLACEDURATION${testIndex}").toFloat(),
                    this.getValue("${prefix}FINDDURATION${testIndex}").toFloat(),
                    this.getValue("${prefix}ANGULARCHANGEF${testIndex}").toFloat(),
                    this.getValue("${prefix}WALKTURNANALYSISF${testIndex}").toFloat(),
                    this.getValue("${prefix}DCOMPLEXITYF${testIndex}").toFloat()
                    )
        }
    }

    private fun Map<String, String>.getTrailMeans(prefix: String): AltoidaTrial {
        return AltoidaTrial(
                this.getValue("${prefix}PLACEDURATIONS").toFloat(),
                this.getValue("${prefix}FINDDURATIONS").toFloat(),
                null,
                this.getValue("${prefix}WALKTURNANALYSISF").toFloat(),
                null
        )
    }

    private fun Map<String, String>.getWalkingTestAggregate(prefix: String, type: AltoidaWalkingTestTypes): AltoidaWalkingTestAggregate {
        val testPrefix = "${prefix}NORM${type.testName}"

        return AltoidaWalkingTestAggregate(
                this.getValue("${testPrefix}MEAN").toFloat(),
                this.getValue("${testPrefix}VARIANCE").toFloat(),
                this.getValue("${testPrefix}TIMERATIOWITHIN").toFloat(),
                this.getValue("${testPrefix}DISTRATIOWITHIN").toFloat(),
                this.getValue("${testPrefix}DISTWITHIN").toFloat(),
                this.getValue("${testPrefix}DISTTOTAL").toFloat(),
                if (type === Serpentine) null else this.getValue("${testPrefix}ANGLESUM").toFloat(),
                if (type === Serpentine) null else this.getValue("${testPrefix}DIRECTIONALCHANGES").toInt(),
                if (type === Serpentine) this["${testPrefix}YPROGRESS"]?.toFloat() else null,
                this.getValue("${testPrefix}SPEED").toFloat(),
                this.getValue("${testPrefix}SPEEDACCURACYRATIO").toFloat(),
                this.getValue("${testPrefix}DURATION").toFloat(),
                this.getValue("${testPrefix}FULLDURATION").toFloat()
        )
    }

    private fun Map<String, String>.getTappingTestAggregate(prefix: String, type: AltoidaTappingTestTypes): AltoidaTappingTestAggregate {
        val testPrefix = "${prefix}NORM${type.testName}"
        return AltoidaTappingTestAggregate(
                this.getValue("${testPrefix}MEAN").toFloat(),
                this.getValue("${testPrefix}VARIANCE").toFloat(),
                this.getValue("${testPrefix}COUNT").toInt(),
                this.getValue("${testPrefix}REACTIONTIMES").toFloat(),
                this.getValue("${testPrefix}RATE").toFloat(),
                this.getValue("${testPrefix}DURATION").toFloat(),
                this.getValue("${testPrefix}FULLDURATION").toFloat()
        )
    }

    private enum class AltoidaWalkingTestTypes(val testName: String) {
        Circle("CIRCLE"),
        Square("SQUARE"),
        Serpentine("SERPENTINE"),
        SpeedCircle("SPEEDCIRCLE")
    }

    private enum class AltoidaTappingTestTypes(val testName: String) {
        RandomTapping("RANDOMTAPPING"),
        Tapping("TAPPING")
    }
}
