package org.radarbase.connect.upload.converter.altoida.summary

import org.radarbase.connect.upload.converter.altoida.summary.AltoidaTestMetricsProcessor.AltoidaWalkingTestTypes.*
import org.radarbase.connect.upload.converter.altoida.summary.AltoidaTestMetricsProcessor.AltoidaTappingTestTypes.*
import org.radarbase.connect.upload.converter.altoida.summary.AltoidaExportCsvProcessor.AltoidaTestCategory

import org.apache.avro.generic.IndexedRecord
import org.radarbase.connect.upload.converter.altoida.AltoidaSummaryLineToRecordMapper
import org.radarcns.connector.upload.altoida.AltoidaSummaryMetrics
import org.radarcns.connector.upload.altoida.AltoidaTappingTestAggregate
import org.radarcns.connector.upload.altoida.AltoidaTrial
import org.radarcns.connector.upload.altoida.AltoidaWalkingTestAggregate

class AltoidaTestMetricsProcessor(private val type: AltoidaTestCategory, override val topic: String) : AltoidaSummaryLineToRecordMapper {
    override fun processLine(line: Map<String, String>, timeReceived: Double): Pair<String, IndexedRecord>? {
        return topic to getTestMetrics(type, line)
    }

    private fun getTestMetrics(type: AltoidaTestCategory, line: Map<String, String>): AltoidaSummaryMetrics {

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
                line.getValue(prefix + "INTROREADTIMES1").toFloat(),
                line.getValue(prefix + "INTROREADTIMES2").toFloat(),
                line.getValue(prefix + "PLACEDELAYS").toFloat(),
                line.getValue(prefix + "SPOTALREADYTAKENCOUNT").toInt(),
                convertTrails(prefix, line),
                getTrailMeans(prefix, line),
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
                getWalkingTestAggregate(prefix, Circle, line),
                getWalkingTestAggregate(prefix, Square, line),
                getWalkingTestAggregate(prefix, Serpentine, line),
                getWalkingTestAggregate(prefix, SpeedCircle, line),
                getTappingTestAggregate(prefix, RandomTapping, line),
                getTappingTestAggregate(prefix, Tapping, line)
        )
    }

    private fun convertTrails(prefix: String, line: Map<String, String>): List<AltoidaTrial> {
        return mutableListOf(AltoidaTrial())
    }

    private fun getTrailMeans(prefix: String, line: Map<String, String>): AltoidaTrial {
        return AltoidaTrial()
    }

    private fun getWalkingTestAggregate(prefix: String, type: AltoidaWalkingTestTypes, line: Map<String, String>): AltoidaWalkingTestAggregate {
        return AltoidaWalkingTestAggregate()
    }

    private fun getTappingTestAggregate(prefix: String, type: AltoidaTappingTestTypes, line: Map<String, String>): AltoidaTappingTestAggregate {
        return AltoidaTappingTestAggregate()
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
