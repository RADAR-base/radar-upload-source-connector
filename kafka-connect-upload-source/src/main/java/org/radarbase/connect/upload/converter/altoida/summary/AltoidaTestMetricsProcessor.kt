package org.radarbase.connect.upload.converter.altoida.summary

import org.apache.avro.generic.IndexedRecord
import org.radarbase.connect.upload.converter.altoida.AltoidaSummaryLineToRecordMapper
import org.radarbase.connect.upload.converter.altoida.summary.AltoidaExportCsvProcessor.AltoidaTestCategory
import org.radarbase.connect.upload.converter.altoida.summary.AltoidaTestMetricsProcessor.AltoidaTappingTestTypes.RandomTapping
import org.radarbase.connect.upload.converter.altoida.summary.AltoidaTestMetricsProcessor.AltoidaTappingTestTypes.Tapping
import org.radarbase.connect.upload.converter.altoida.summary.AltoidaTestMetricsProcessor.AltoidaWalkingTestTypes.*
import org.radarcns.connector.upload.altoida.AltoidaSummaryMetrics
import org.radarcns.connector.upload.altoida.AltoidaTappingTestAggregate
import org.radarcns.connector.upload.altoida.AltoidaTrial
import org.radarcns.connector.upload.altoida.AltoidaWalkingTestAggregate

class AltoidaTestMetricsProcessor(private val type: AltoidaTestCategory, override val topic: String) : AltoidaSummaryLineToRecordMapper {
    override fun processLine(line: Map<String, String>, timeReceived: Double): Pair<String, IndexedRecord>? {
        return topic to getTestMetrics(type, line)
    }

    private fun getTestMetrics(type: AltoidaTestCategory, line: Map<String, String>): IndexedRecord {

        val prefix = if (type === AltoidaTestCategory.BIT) "BIT_" else "DOT_"

        return AltoidaSummaryMetrics(
                line.getValue(prefix + "PLAYHIGHREACTIONTIMES").toFloat(),
                line.getValue(prefix + "PLAYHIGHACCURACY").toFloat(),
                line.getValue(prefix + "PLAYLOWREACTIONS").toInt(),
                line.getValue(prefix + "IGNOREDHIGHTONEPERCENTAGE").toFloat(),
                line.getValue(prefix + "PREMATURETONEBUTTONPRESSES").toInt(),
                line.getValue(prefix + "RANDOMSCREENPRESSESDURINGPLACEMENT").toInt(),
                line.getValue(prefix + "RANDOMSCREENPRESSESDURINGSEARCH").toInt(),
                line.getValue(prefix + "TOOMUCHMOVEMENTCOUNT").toInt(),
                line.getValue(prefix + "FINDBETTERPLACECOUNT").toFloat(),
                line.getValue(prefix + "INTROREADTIMES").toFloat(),
                line.getValue(prefix + "INTROREADTIME1").toFloat(),
                line.getValue(prefix + "INTROREADTIME2").toFloat(),
                line.getValue(prefix + "PLACEDELAYS").toFloat(),
                line.getValue(prefix + "SPOTALREADYTAKENCOUNT").toInt(),
                line.getTrails(prefix),
                line.getTrailMeans(prefix),
                line.getValue(prefix + "FINDFAILCOUNT").toFloat(),
                line.getValue(prefix + "FINDSKIPDURATIONS").toFloat(),
                line.getValue(prefix + "SKIPBUTTONCOUNT").toFloat(),
                line.getValue(prefix + "COUNTDOWNFAIL").toFloat(),
                line.getValue(prefix + "STEPCOUNTPFRATIO").toFloat(),
                line.getValue(prefix + "MEANSTEPDELAYP").toFloat(),
                line.getValue(prefix + "MEANSTEPDELAYF").toFloat(),
                line.getValue(prefix + "STEPVARIANCEP").toFloat(),
                line.getValue(prefix + "STEPVARIANCEF").toFloat(),
                line.getValue(prefix + "NOTWALKINGTIMEP").toFloat(),
                line.getValue(prefix + "NOTWALKINGTIMEF").toFloat(),
                line.getValue(prefix + "SHOCKCOUNT").toFloat(),
                line.getValue(prefix + "ACCVARIANCE1").toFloat(),
                line.getValue(prefix + "ACCVARIANCE2").toFloat(),
                line.getValue(prefix + "ACCVARIANCE3").toFloat(),
                line.getValue(prefix + "STRONGHAND").toFloat(),
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
                    this.getValue(prefix + "PLACEDURATION" + testIndex).toFloat(),
                    this.getValue(prefix + "FINDDURATION" + testIndex).toFloat(),
                    this.getValue(prefix + "ANGULARCHANGEF" + testIndex).toFloat(),
                    this.getValue(prefix + "WALKTURNANALYSISF" + testIndex).toFloat(),
                    this.getValue(prefix + "DCOMPLEXITYF" + testIndex).toFloat()
                    )
        }
    }

    private fun Map<String, String>.getTrailMeans(prefix: String): AltoidaTrial {
        return AltoidaTrial(
                this.getValue(prefix + "PLACEDURATIONS").toFloat(),
                this.getValue(prefix + "FINDDURATIONS").toFloat(),
                null, // TODO this should be null
                this.getValue(prefix + "WALKTURNANALYSISF").toFloat(),
                null // TODO this should be null
        )
    }

    private fun Map<String, String>.getWalkingTestAggregate(prefix: String, type: AltoidaWalkingTestTypes): AltoidaWalkingTestAggregate {
        val testName = when (type) {
            Circle -> "CIRCLE"
            Square -> "SQUARE"
            Serpentine -> "SERPENTINE"
            SpeedCircle -> "SPEEDCIRCLE"
        }

        return AltoidaWalkingTestAggregate(
                this.getValue(prefix + "NORM" + testName + "MEAN").toFloat(),
                this.getValue(prefix + "NORM" + testName + "VARIANCE").toFloat(),
                this.getValue(prefix + "NORM" + testName + "TIMERATIOWITHIN").toFloat(),
                this.getValue(prefix + "NORM" + testName + "DISTRATIOWITHIN").toFloat(),
                this.getValue(prefix + "NORM" + testName + "DISTWITHIN").toFloat(),
                this.getValue(prefix + "NORM" + testName + "DISTTOTAL").toFloat(),
                if (type === Serpentine) null else this.getValue(prefix + "NORM" + testName + "ANGLESUM").toFloat(),
                if (type === Serpentine) null else this.getValue(prefix + "NORM" + testName + "DIRECTIONALCHANGES").toInt(),
                if (type === Serpentine) this.getValue(prefix + "NORM" + testName + "YPROGRESS")?.toFloat() else null,
                this.getValue(prefix + "NORM" + testName + "SPEED").toFloat(),
                this.getValue(prefix + "NORM" + testName + "SPEEDACCURACYRATIO").toFloat(),
                this.getValue(prefix + "NORM" + testName + "DURATION").toFloat(),
                this.getValue(prefix + "NORM" + testName + "FULLDURATION").toFloat()
        )
    }

    private fun Map<String, String>.getTappingTestAggregate(prefix: String, type: AltoidaTappingTestTypes): AltoidaTappingTestAggregate {
        val testType = if (type === RandomTapping) "RANDOMTAPPING" else "TAPPING"
        return AltoidaTappingTestAggregate(
                this.getValue(prefix + "NORM" + testType + "MEAN").toFloat(),
                this.getValue(prefix + "NORM" + testType + "VARIANCE").toFloat(),
                this.getValue(prefix + "NORM" + testType + "COUNT").toInt(),
                this.getValue(prefix + "NORM" + testType + "REACTIONTIMES").toFloat(),
                this.getValue(prefix + "NORM" + testType + "RATE").toFloat(),
                this.getValue(prefix + "NORM" + testType + "DURATION").toFloat(),
                this.getValue(prefix + "NORM" + testType + "FULLDURATION").toFloat()
        )
    }

    private enum class AltoidaWalkingTestTypes {
        Circle,
        Square,
        Serpentine,
        SpeedCircle
    }

    private enum class AltoidaTappingTestTypes {
        RandomTapping,
        Tapping
    }
}
