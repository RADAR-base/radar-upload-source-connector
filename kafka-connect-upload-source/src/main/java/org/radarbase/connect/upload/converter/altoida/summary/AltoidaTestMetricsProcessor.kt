package org.radarbase.connect.upload.converter.altoida.summary

import org.apache.avro.generic.IndexedRecord
import org.radarbase.connect.upload.converter.StatelessCsvLineProcessor
import org.radarbase.connect.upload.converter.TimeFieldParser
import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.altoida.summary.AltoidaTestMetricsProcessor.AltoidaTappingTestTypes.RandomTapping
import org.radarbase.connect.upload.converter.altoida.summary.AltoidaTestMetricsProcessor.AltoidaTappingTestTypes.Tapping
import org.radarbase.connect.upload.converter.altoida.summary.AltoidaTestMetricsProcessor.AltoidaWalkingTestTypes.*
import org.radarcns.connector.upload.altoida.AltoidaSummaryMetrics
import org.radarcns.connector.upload.altoida.AltoidaTappingTestAggregate
import org.radarcns.connector.upload.altoida.AltoidaTrial
import org.radarcns.connector.upload.altoida.AltoidaWalkingTestAggregate

class AltoidaTestMetricsProcessor(private val type: AltoidaTestCategory, val topic: String) : StatelessCsvLineProcessor() {
    override val fileNameSuffix: String = "export.csv"

    override val timeFieldParser: TimeFieldParser = AltoidaSummaryProcessor.defaultTimeFormatter

    override val header: List<String> = listOf(
            "${type.prefix}AR_HIGHTONEREACTIONTIMES",
            "${type.prefix}AR_HIGHTONETOUCHACCURACY",
            "${type.prefix}AR_IGNOREDHIGHTONEPERCENTAGE",
            "${type.prefix}AR_PREMATURETONEBUTTONPRESSES",
            "${type.prefix}AR_RANDOMSCREENPRESSESDURINGPLACEMENT",
            "${type.prefix}AR_RANDOMSCREENPRESSESDURINGSEARCH",
            "${type.prefix}AR_TOOMUCHMOVEMENTCOUNT",
            "${type.prefix}AR_FINDBETTERPLACECOUNT",
            "${type.prefix}AR_INTROREADTIMES",
            "${type.prefix}AR_INTROREADTIME1",
            "${type.prefix}AR_INTROREADTIME2",
            "${type.prefix}AR_PLACEDELAYSAVERAGE",
            "${type.prefix}AR_PLACEDURATIONSAVERAGE",
            "${type.prefix}AR_PLACEDURATIONOBJ1",
            "${type.prefix}AR_PLACEDURATIONOBJ2",
            "${type.prefix}AR_PLACEDURATIONOBJ3",
            "${type.prefix}AR_SPOTALREADYTAKENCOUNT",
            "${type.prefix}AR_FINDDURATIONSAVERAGE",
            "${type.prefix}AR_FINDDURATIONOBJ1",
            "${type.prefix}AR_FINDDURATIONOBJ2",
            "${type.prefix}AR_FINDDURATIONOBJ3",
            "${type.prefix}AR_FINDFAILCOUNT",
            "${type.prefix}AR_FINDSKIPDURATIONS",
            "${type.prefix}AR_SKIPBUTTONCOUNT",
            "${type.prefix}AR_COUNTDOWNFAIL",
            "${type.prefix}AR_ANGULARCHANGEFINDOBJ1",
            "${type.prefix}AR_ANGULARCHANGEFINDOBJ2",
            "${type.prefix}AR_ANGULARCHANGEFINDOBJ3",
            "${type.prefix}AR_WALKTURNANALYSISFINDOBJ1",
            "${type.prefix}AR_WALKTURNANALYSISFINDOBJ2",
            "${type.prefix}AR_WALKTURNANALYSISFINDOBJ3",
            "${type.prefix}AR_WALKTURNANALYSISFINDAVG",
            "${type.prefix}AR_PATHCOMPLEXITYFINDOBJ1",
            "${type.prefix}AR_PATHCOMPLEXITYFINDOBJ2",
            "${type.prefix}AR_PATHCOMPLEXITYFINDOBJ3",
            "${type.prefix}AR_STEPCOUNTFINDPLACERATIO",
            "${type.prefix}AR_PLACINGMEANSTEPDELAY",
            "${type.prefix}AR_FINDINGMEANSTEPDELAY",
            "${type.prefix}AR_PLACINGSTEPVARIANCE",
            "${type.prefix}AR_FINDINGSTEPVARIANCE",
            "${type.prefix}AR_PLACINGNOTWALKINGTIME",
            "${type.prefix}AR_FINDINGNOTWALKINGTIME",
            "${type.prefix}AR_SHOCKCOUNT",
            "${type.prefix}AR_ACCVARIANCEX",
            "${type.prefix}AR_ACCVARIANCEY",
            "${type.prefix}AR_ACCVARIANCEZ",
            "DOMINANTHAND",
            "${type.prefix}MOTOR_CIRCLE_DEVIATIONMEAN",
            "${type.prefix}MOTOR_CIRCLE_DEVIATIONVARIANCE",
            "${type.prefix}MOTOR_CIRCLE_TIMERATIOWITHIN",
            "${type.prefix}MOTOR_CIRCLE_DISTRATIOWITHIN",
            "${type.prefix}MOTOR_CIRCLE_DISTWITHIN",
            "${type.prefix}MOTOR_CIRCLE_DISTTOTAL",
            "${type.prefix}MOTOR_CIRCLE_ANGLESUM",
            "${type.prefix}MOTOR_CIRCLE_SPEED",
            "${type.prefix}MOTOR_CIRCLE_SPEEDACCURACYRATIO",
            "${type.prefix}MOTOR_CIRCLE_DURATION",
            "${type.prefix}MOTOR_CIRCLE_FULLDURATION",
            "${type.prefix}MOTOR_SQUARE_DEVIATIONMEAN",
            "${type.prefix}MOTOR_SQUARE_DEVIATIONVARIANCE",
            "${type.prefix}MOTOR_SQUARE_TIMERATIOWITHIN",
            "${type.prefix}MOTOR_SQUARE_DISTRATIOWITHIN",
            "${type.prefix}MOTOR_SQUARE_DISTWITHIN",
            "${type.prefix}MOTOR_SQUARE_DISTTOTAL",
            "${type.prefix}MOTOR_SQUARE_ANGLESUM",
            "${type.prefix}MOTOR_SQUARE_SPEED",
            "${type.prefix}MOTOR_SQUARE_SPEEDACCURACYRATIO",
            "${type.prefix}MOTOR_SQUARE_DURATION",
            "${type.prefix}MOTOR_SQUARE_FULLDURATION",
            "${type.prefix}MOTOR_SERPENTINE_DEVIATIONMEAN",
            "${type.prefix}MOTOR_SERPENTINE_DEVIATIONVARIANCE",
            "${type.prefix}MOTOR_SERPENTINE_TIMERATIOWITHIN",
            "${type.prefix}MOTOR_SERPENTINE_DISTRATIOWITHIN",
            "${type.prefix}MOTOR_SERPENTINE_DISTWITHIN",
            "${type.prefix}MOTOR_SERPENTINE_DISTTOTAL",
            "${type.prefix}MOTOR_SERPENTINE_YPROGRESS",
            "${type.prefix}MOTOR_SERPENTINE_SPEED",
            "${type.prefix}MOTOR_SERPENTINE_SPEEDACCURACYRATIO",
            "${type.prefix}MOTOR_SERPENTINE_DURATION",
            "${type.prefix}MOTOR_SERPENTINE_FULLDURATION",
            "${type.prefix}MOTOR_SPEEDCIRCLE_DEVIATIONMEAN",
            "${type.prefix}MOTOR_SPEEDCIRCLE_DEVIATIONVARIANCE",
            "${type.prefix}MOTOR_SPEEDCIRCLE_TIMERATIOWITHIN",
            "${type.prefix}MOTOR_SPEEDCIRCLE_DISTRATIOWITHIN",
            "${type.prefix}MOTOR_SPEEDCIRCLE_DISTWITHIN",
            "${type.prefix}MOTOR_SPEEDCIRCLE_DISTTOTAL",
            "${type.prefix}MOTOR_SPEEDCIRCLE_ANGLESUM",
            "${type.prefix}MOTOR_SPEEDCIRCLE_SPEED",
            "${type.prefix}MOTOR_SPEEDCIRCLE_SPEEDACCURACYRATIO",
            "${type.prefix}MOTOR_SPEEDCIRCLE_DURATION",
            "${type.prefix}MOTOR_SPEEDCIRCLE_FULLDURATION",
            "${type.prefix}MOTOR_RANDOMTAPPING_DEVIATIONMEAN",
            "${type.prefix}MOTOR_RANDOMTAPPING_DEVIATIONVARIANCE",
            "${type.prefix}MOTOR_RANDOMTAPPING_NUMBEROFTAPS",
            "${type.prefix}MOTOR_RANDOMTAPPING_REACTIONTIMES",
            "${type.prefix}MOTOR_RANDOMTAPPING_DURATION",
            "${type.prefix}MOTOR_RANDOMTAPPING_TAPSPERSECOND",
            "${type.prefix}MOTOR_RANDOMTAPPING_FULLDURATION",
            "${type.prefix}MOTOR_TAPPING_DEVIATIONMEAN",
            "${type.prefix}MOTOR_TAPPING_DEVIATIONVARIANCE",
            "${type.prefix}MOTOR_TAPPING_NUMBEROFTAPS",
            "${type.prefix}MOTOR_TAPPING_REACTIONTIMES",
            "${type.prefix}MOTOR_TAPPING_TAPSPERSECOND",
            "${type.prefix}MOTOR_TAPPING_DURATION",
            "${type.prefix}MOTOR_TAPPING_FULLDURATION"
            )

    override fun lineConversion(line: Map<String, String>, timeReceived: Double) = TopicData(
            topic,
            getTestMetrics(line, timeReceived))

    private fun getTestMetrics(line: Map<String, String>, timeReceived: Double): IndexedRecord {
        val prefix = type.prefix
        val prefixAR = "${prefix}AR_"
        val prefixMotor = "${prefix}MOTOR_"
        val defaultVal = "0"

        return AltoidaSummaryMetrics(
                time(line),
                timeReceived,
                line.getValue("${prefixAR}HIGHTONEREACTIONTIMES").toFloat(),
                line.getValue("${prefixAR}HIGHTONETOUCHACCURACY").toFloat(),
                line.getOrDefault("${prefixAR}NMRREACTIONSTOLOWTONE", defaultVal).toInt(),
                line.getValue("${prefixAR}IGNOREDHIGHTONEPERCENTAGE").toFloat(),
                line.getValue("${prefixAR}PREMATURETONEBUTTONPRESSES").toInt(),
                line.getValue("${prefixAR}RANDOMSCREENPRESSESDURINGPLACEMENT").toInt(),
                line.getValue("${prefixAR}RANDOMSCREENPRESSESDURINGSEARCH").toInt(),
                line.getValue("${prefixAR}TOOMUCHMOVEMENTCOUNT").toInt(),
                line.getValue("${prefixAR}FINDBETTERPLACECOUNT").toFloat(),
                line.getValue("${prefixAR}INTROREADTIMES").toFloat(),
                line.getValue("${prefixAR}INTROREADTIME1").toFloat(),
                line.getValue("${prefixAR}INTROREADTIME2").toFloat(),
                line.getValue("${prefixAR}PLACEDELAYSAVERAGE").toFloat(),
                line.getValue("${prefixAR}SPOTALREADYTAKENCOUNT").toInt(),
                line.getTrails(prefixAR),
                line.getTrailMeans(prefixAR),
                line.getValue("${prefixAR}FINDFAILCOUNT").toFloat(),
                line.getValue("${prefixAR}FINDSKIPDURATIONS").toFloat(),
                line.getValue("${prefixAR}SKIPBUTTONCOUNT").toFloat(),
                line.getValue("${prefixAR}COUNTDOWNFAIL").toFloat(),
                line.getValue("${prefixAR}STEPCOUNTFINDPLACERATIO").toFloat(),
                line.getValue("${prefixAR}PLACINGMEANSTEPDELAY").toFloat(),
                line.getValue("${prefixAR}FINDINGMEANSTEPDELAY").toFloat(),
                line.getValue("${prefixAR}PLACINGSTEPVARIANCE").toFloat(),
                line.getValue("${prefixAR}FINDINGSTEPVARIANCE").toFloat(),
                line.getValue("${prefixAR}PLACINGNOTWALKINGTIME").toFloat(),
                line.getValue("${prefixAR}FINDINGNOTWALKINGTIME").toFloat(),
                line.getValue("${prefixAR}SHOCKCOUNT").toFloat(),
                line.getValue("${prefixAR}ACCVARIANCEX").toFloat(),
                line.getValue("${prefixAR}ACCVARIANCEY").toFloat(),
                line.getValue("${prefixAR}ACCVARIANCEZ").toFloat(),
                line.getValue("DOMINANTHAND").toFloat(),
                line.getWalkingTestAggregate(prefixMotor, Circle),
                line.getWalkingTestAggregate(prefixMotor, Square),
                line.getWalkingTestAggregate(prefixMotor, Serpentine),
                line.getWalkingTestAggregate(prefixMotor, SpeedCircle),
                line.getTappingTestAggregate(prefixMotor, RandomTapping),
                line.getTappingTestAggregate(prefixMotor, Tapping)
        )
    }

    private fun Map<String, String>.getTrails(prefix: String): List<AltoidaTrial> {
        return listOf("1", "2", "3").map { testIndex ->
            AltoidaTrial(
                    this.getValue("${prefix}PLACEDURATIONOBJ${testIndex}").toFloat(),
                    this.getValue("${prefix}FINDDURATIONOBJ${testIndex}").toFloat(),
                    this.getValue("${prefix}ANGULARCHANGEFINDOBJ${testIndex}").toFloat(),
                    this.getValue("${prefix}WALKTURNANALYSISFINDOBJ${testIndex}").toFloat(),
                    this.getValue("${prefix}PATHCOMPLEXITYFINDOBJ${testIndex}").toFloat()
                    )
        }
    }

    private fun Map<String, String>.getTrailMeans(prefix: String): AltoidaTrial {
        return AltoidaTrial(
                this.getValue("${prefix}PLACEDURATIONSAVERAGE").toFloat(),
                this.getValue("${prefix}FINDDURATIONSAVERAGE").toFloat(),
                null,
                this.getValue("${prefix}WALKTURNANALYSISFINDAVG").toFloat(),
                null
        )
    }

    private fun Map<String, String>.getWalkingTestAggregate(prefix: String, type: AltoidaWalkingTestTypes): AltoidaWalkingTestAggregate {
        val testPrefix = "${prefix}${type.testName}_"

        return AltoidaWalkingTestAggregate(
                this.getValue("${testPrefix}DEVIATIONMEAN").toFloat(),
                this.getValue("${testPrefix}DEVIATIONVARIANCE").toFloat(),
                this.getValue("${testPrefix}TIMERATIOWITHIN").toFloat(),
                this.getValue("${testPrefix}DISTRATIOWITHIN").toFloat(),
                this.getValue("${testPrefix}DISTWITHIN").toFloat(),
                this.getValue("${testPrefix}DISTTOTAL").toFloat(),
                if (type === Serpentine) null else this.getValue("${testPrefix}ANGLESUM").toFloat(),
                null,
                if (type === Serpentine) this["${testPrefix}YPROGRESS"]?.toFloat() else null,
                this.getValue("${testPrefix}SPEED").toFloat(),
                this.getValue("${testPrefix}SPEEDACCURACYRATIO").toFloat(),
                this.getValue("${testPrefix}DURATION").toFloat(),
                this.getValue("${testPrefix}FULLDURATION").toFloat()
        )
    }

    private fun Map<String, String>.getTappingTestAggregate(prefix: String, type: AltoidaTappingTestTypes): AltoidaTappingTestAggregate {
        val testPrefix = "${prefix}${type.testName}_"

        return AltoidaTappingTestAggregate(
                this.getValue("${testPrefix}DEVIATIONMEAN").toFloat(),
                this.getValue("${testPrefix}DEVIATIONVARIANCE").toFloat(),
                this.getValue("${testPrefix}NUMBEROFTAPS").toInt(),
                this.getValue("${testPrefix}REACTIONTIMES").toFloat(),
                this.getValue("${testPrefix}TAPSPERSECOND").toFloat(),
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

    enum class AltoidaTestCategory(val prefix: String) {
        DOT("DOT_"),
        BIT("BIT_")
    }
}
