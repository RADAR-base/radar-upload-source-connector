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
            "${type.prefix}PLAYHIGHREACTIONTIMES",
            "${type.prefix}PLAYHIGHACCURACY",
            "${type.prefix}PLAYLOWREACTIONS",
            "${type.prefix}IGNOREDHIGHTONEPERCENTAGE",
            "${type.prefix}PREMATURETONEBUTTONPRESSES",
            "${type.prefix}RANDOMSCREENPRESSESDURINGPLACEMENT",
            "${type.prefix}RANDOMSCREENPRESSESDURINGSEARCH",
            "${type.prefix}TOOMUCHMOVEMENTCOUNT",
            "${type.prefix}FINDBETTERPLACECOUNT",
            "${type.prefix}INTROREADTIMES",
            "${type.prefix}INTROREADTIME1",
            "${type.prefix}INTROREADTIME2",
            "${type.prefix}PLACEDELAYS",
            "${type.prefix}PLACEDURATIONS",
            "${type.prefix}PLACEDURATION1",
            "${type.prefix}PLACEDURATION2",
            "${type.prefix}PLACEDURATION3",
            "${type.prefix}SPOTALREADYTAKENCOUNT",
            "${type.prefix}FINDDURATIONS",
            "${type.prefix}FINDDURATION1",
            "${type.prefix}FINDDURATION2",
            "${type.prefix}FINDDURATION3",
            "${type.prefix}FINDFAILCOUNT",
            "${type.prefix}FINDSKIPDURATIONS",
            "${type.prefix}SKIPBUTTONCOUNT",
            "${type.prefix}COUNTDOWNFAIL",
            "${type.prefix}ANGULARCHANGEF1",
            "${type.prefix}ANGULARCHANGEF2",
            "${type.prefix}ANGULARCHANGEF3",
            "${type.prefix}WALKTURNANALYSISF1",
            "${type.prefix}WALKTURNANALYSISF2",
            "${type.prefix}WALKTURNANALYSISF3",
            "${type.prefix}WALKTURNANALYSISF",
            "${type.prefix}DCOMPLEXITYF1",
            "${type.prefix}DCOMPLEXITYF2",
            "${type.prefix}DCOMPLEXITYF3",
            "${type.prefix}STEPCOUNTPFRATIO",
            "${type.prefix}MEANSTEPDELAYP",
            "${type.prefix}MEANSTEPDELAYF",
            "${type.prefix}STEPVARIANCEP",
            "${type.prefix}STEPVARIANCEF",
            "${type.prefix}NOTWALKINGTIMEP",
            "${type.prefix}NOTWALKINGTIMEF",
            "${type.prefix}SHOCKCOUNT",
            "${type.prefix}ACCVARIANCE1",
            "${type.prefix}ACCVARIANCE2",
            "${type.prefix}ACCVARIANCE3",
            "${type.prefix}STRONGHAND",
            "${type.prefix}NORMCIRCLEMEAN",
            "${type.prefix}NORMCIRCLEVARIANCE",
            "${type.prefix}NORMCIRCLETIMERATIOWITHIN",
            "${type.prefix}NORMCIRCLEDISTRATIOWITHIN",
            "${type.prefix}NORMCIRCLEDISTWITHIN",
            "${type.prefix}NORMCIRCLEDISTTOTAL",
            "${type.prefix}NORMCIRCLEANGLESUM",
            "${type.prefix}NORMCIRCLEDIRECTIONALCHANGES",
            "${type.prefix}NORMCIRCLESPEED",
            "${type.prefix}NORMCIRCLESPEEDACCURACYRATIO",
            "${type.prefix}NORMCIRCLEDURATION",
            "${type.prefix}NORMCIRCLEFULLDURATION",
            "${type.prefix}NORMSQUAREMEAN",
            "${type.prefix}NORMSQUAREVARIANCE",
            "${type.prefix}NORMSQUARETIMERATIOWITHIN",
            "${type.prefix}NORMSQUAREDISTRATIOWITHIN",
            "${type.prefix}NORMSQUAREDISTWITHIN",
            "${type.prefix}NORMSQUAREDISTTOTAL",
            "${type.prefix}NORMSQUAREANGLESUM",
            "${type.prefix}NORMSQUAREDIRECTIONALCHANGES",
            "${type.prefix}NORMSQUARESPEED",
            "${type.prefix}NORMSQUARESPEEDACCURACYRATIO",
            "${type.prefix}NORMSQUAREDURATION",
            "${type.prefix}NORMSQUAREFULLDURATION",
            "${type.prefix}NORMSERPENTINEMEAN",
            "${type.prefix}NORMSERPENTINEVARIANCE",
            "${type.prefix}NORMSERPENTINETIMERATIOWITHIN",
            "${type.prefix}NORMSERPENTINEDISTRATIOWITHIN",
            "${type.prefix}NORMSERPENTINEDISTWITHIN",
            "${type.prefix}NORMSERPENTINEDISTTOTAL",
            "${type.prefix}NORMSERPENTINEYPROGRESS",
            "${type.prefix}NORMSERPENTINESPEED",
            "${type.prefix}NORMSERPENTINESPEEDACCURACYRATIO",
            "${type.prefix}NORMSERPENTINEDURATION",
            "${type.prefix}NORMSERPENTINEFULLDURATION",
            "${type.prefix}NORMSPEEDCIRCLEMEAN",
            "${type.prefix}NORMSPEEDCIRCLEVARIANCE",
            "${type.prefix}NORMSPEEDCIRCLETIMERATIOWITHIN",
            "${type.prefix}NORMSPEEDCIRCLEDISTRATIOWITHIN",
            "${type.prefix}NORMSPEEDCIRCLEDISTWITHIN",
            "${type.prefix}NORMSPEEDCIRCLEDISTTOTAL",
            "${type.prefix}NORMSPEEDCIRCLEANGLESUM",
            "${type.prefix}NORMSPEEDCIRCLEDIRECTIONALCHANGES",
            "${type.prefix}NORMSPEEDCIRCLESPEED",
            "${type.prefix}NORMSPEEDCIRCLESPEEDACCURACYRATIO",
            "${type.prefix}NORMSPEEDCIRCLEDURATION",
            "${type.prefix}NORMSPEEDCIRCLEFULLDURATION",
            "${type.prefix}NORMRANDOMTAPPINGMEAN",
            "${type.prefix}NORMRANDOMTAPPINGVARIANCE",
            "${type.prefix}NORMRANDOMTAPPINGCOUNT",
            "${type.prefix}NORMRANDOMTAPPINGREACTIONTIMES",
            "${type.prefix}NORMRANDOMTAPPINGDURATION",
            "${type.prefix}NORMRANDOMTAPPINGRATE",
            "${type.prefix}NORMRANDOMTAPPINGFULLDURATION",
            "${type.prefix}NORMTAPPINGMEAN",
            "${type.prefix}NORMTAPPINGVARIANCE",
            "${type.prefix}NORMTAPPINGCOUNT",
            "${type.prefix}NORMTAPPINGREACTIONTIMES",
            "${type.prefix}NORMTAPPINGRATE",
            "${type.prefix}NORMTAPPINGDURATION",
            "${type.prefix}NORMTAPPINGFULLDURATION")

    override fun lineConversion(line: Map<String, String>, timeReceived: Double) = TopicData(
            topic,
            getTestMetrics(line, timeReceived))

    private fun getTestMetrics(line: Map<String, String>, timeReceived: Double): IndexedRecord {
        val prefix = type.prefix

        return AltoidaSummaryMetrics(
                time(line),
                timeReceived,
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

    enum class AltoidaTestCategory(val prefix: String) {
        DOT("DOT_"),
        BIT("BIT_")
    }
}
