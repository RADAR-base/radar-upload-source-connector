/*
 *
 *  * Copyright 2019 The Hyve
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */

package org.radarbase.connect.upload.converter.altoida.summary

import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder
import com.opencsv.CSVWriter
import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.FilePreProcessor
import org.radarbase.connect.upload.converter.FilePreProcessorFactory
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * AltoidaSummaryCsvPreProcesor allows the preprocessing of the export.csv InputStream
 * before the data is converted into kafka records. This is needed because many export.csv files
 * have incorrect headers (some header fields are empty or incorrectly matched with the values),
 * even when the content/values are correct. This replaces the whole header and replaces it with
 * the correct expected header.
 */
class AltoidaSummaryCsvPreProcessorFactory : FilePreProcessorFactory {
    override fun matches(contents: ContentsDTO): Boolean = contents.fileName.endsWith("export.csv")

    override fun createPreProcessor(record: RecordDTO): FilePreProcessor = AltoidaSummaryCsvPreProcessor()

    private inner class AltoidaSummaryCsvPreProcessor : FilePreProcessor {
        override fun preProcessFile(
            context: ConverterFactory.ContentsContext,
            inputStream: InputStream,
        ): InputStream {
            logger.info("Converting input stream..")
            return inputStream.bufferedReader().toCsvReader().use { reader ->
                var header = reader.readNext()?.map { it }
                    .takeIf { !it.isNullOrEmpty() }
                    .orEmpty()
                    .filter { h -> h.isNotEmpty() }

                if (header.size < fileHeader.size) header = fileHeader
                val line = reader.readNext()
                val outputStream = ByteArrayOutputStream()
                val writer = CSVWriter(outputStream.writer(StandardCharsets.UTF_8))

                writer.writeNext(header.toTypedArray())
                writer.writeNext(line)
                writer.flush()
                writer.close()
                outputStream.flush()
                outputStream.close()

                ByteArrayInputStream(outputStream.toByteArray())
            }
        }

        private fun BufferedReader.toCsvReader(): CSVReader = CSVReaderBuilder(this)
            .withCSVParser(CSVParserBuilder().withSeparator(',').build())
            .build()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AltoidaSummaryCsvPreProcessorFactory::class.java)

        /** Expected file header list (uppercase). */
        val fileHeader = listOf(
            "LABEL",
            "TIMESTAMP",
            "CLASS",
            "NMI",
            "DOMAINPERCENTILE_PERCEPTUALMOTORCOORDINATION",
            "DOMAINPERCENTILE_COMPLEXATTENTION",
            "DOMAINPERCENTILE_COGNITIVEPROCESSINGSPEED",
            "DOMAINPERCENTILE_INHIBITION",
            "DOMAINPERCENTILE_FLEXIBILITY",
            "DOMAINPERCENTILE_VISUALPERCEPTION",
            "DOMAINPERCENTILE_PLANNING",
            "DOMAINPERCENTILE_PROSPECTIVEMEMORY",
            "DOMAINPERCENTILE_EYEMOVEMENT",
            "DOMAINPERCENTILE_SPEECH",
            "DOMAINPERCENTILE_SPATIALMEMORY",
            "BIT_AR_REMEMBEREDFIRSTITEMPLACEDCORRECTLY",
            "BIT_AR_REMEMBEREDFIRSTITEMSEARCHEDCORRECTLY",
            "DOT_AR_REMEMBEREDFIRSTITEMPLACEDCORRECTLY",
            "DOT_AR_REMEMBEREDFIRSTITEMSEARCHEDCORRECTLY",
            "BIT_AR_HIGHTONEREACTIONTIMES",
            "BIT_AR_HIGHTONETOUCHACCURACY",
            "BIT_AR_RANDOMSCREENPRESSESDURINGPLACEMENT",
            "BIT_AR_RANDOMSCREENPRESSESDURINGSEARCH",
            "BIT_AR_PREMATURETONEBUTTONPRESSES",
            "BIT_AR_IGNOREDHIGHTONEPERCENTAGE",
            "DOT_AR_HIGHTONEREACTIONTIMES",
            "DOT_AR_HIGHTONETOUCHACCURACY",
            "DOT_AR_RANDOMSCREENPRESSESDURINGPLACEMENT",
            "DOT_AR_RANDOMSCREENPRESSESDURINGSEARCH",
            "DOT_AR_PREMATURETONEBUTTONPRESSES",
            "DOT_AR_IGNOREDHIGHTONEPERCENTAGE",
            "DOT_AR_NMRREACTIONSTOLOWTONE",
            "BIT_AR_SHOCKCOUNT",
            "DOT_AR_SHOCKCOUNT",
            "BIT_AR_ACCVARIANCEX",
            "BIT_AR_ACCVARIANCEY",
            "BIT_AR_ACCVARIANCEZ",
            "DOT_AR_ACCVARIANCEX",
            "DOT_AR_ACCVARIANCEY",
            "DOT_AR_ACCVARIANCEZ",
            "AGE",
            "BIT_MOTOR_CIRCLE_DEVIATIONMEAN",
            "BIT_MOTOR_CIRCLE_DEVIATIONVARIANCE",
            "BIT_MOTOR_CIRCLE_TIMERATIOWITHIN",
            "BIT_MOTOR_CIRCLE_DISTRATIOWITHIN",
            "BIT_MOTOR_CIRCLE_DISTWITHIN",
            "BIT_MOTOR_CIRCLE_DISTTOTAL",
            "BIT_MOTOR_CIRCLE_ANGLESUM",
            "BIT_MOTOR_CIRCLE_SPEED",
            "BIT_MOTOR_CIRCLE_SPEEDACCURACYRATIO",
            "BIT_MOTOR_CIRCLE_DURATION",
            "BIT_MOTOR_CIRCLE_FULLDURATION",
            "DOT_MOTOR_CIRCLE_DEVIATIONMEAN",
            "DOT_MOTOR_CIRCLE_DEVIATIONVARIANCE",
            "DOT_MOTOR_CIRCLE_TIMERATIOWITHIN",
            "DOT_MOTOR_CIRCLE_DISTRATIOWITHIN",
            "DOT_MOTOR_CIRCLE_DISTWITHIN",
            "DOT_MOTOR_CIRCLE_DISTTOTAL",
            "DOT_MOTOR_CIRCLE_ANGLESUM",
            "DOT_MOTOR_CIRCLE_SPEED",
            "DOT_MOTOR_CIRCLE_SPEEDACCURACYRATIO",
            "DOT_MOTOR_CIRCLE_DURATION",
            "DOT_MOTOR_CIRCLE_FULLDURATION",
            "BIT_AR_COUNTDOWNFAIL",
            "DOT_AR_COUNTDOWNFAIL",
            "DOMINANTHAND",
            "BIT_AR_FINDBETTERPLACECOUNT",
            "DOT_AR_FINDBETTERPLACECOUNT",
            "BIT_AR_FINDDURATIONSAVERAGE",
            "BIT_AR_FINDDURATIONOBJ1",
            "BIT_AR_FINDDURATIONOBJ2",
            "BIT_AR_FINDDURATIONOBJ3",
            "DOT_AR_FINDDURATIONSAVERAGE",
            "DOT_AR_FINDDURATIONOBJ1",
            "DOT_AR_FINDDURATIONOBJ2",
            "DOT_AR_FINDDURATIONOBJ3",
            "BIT_AR_FINDFAILCOUNT",
            "DOT_AR_FINDFAILCOUNT",
            "BIT_AR_FINDSKIPDURATIONS",
            "DOT_AR_FINDSKIPDURATIONS",
            "GENDER",
            "BIT_HOUROFDAY",
            "DOT_HOUROFDAY",
            "BIT_AR_INITWALK_TIME",
            "BIT_AR_INITWALK_PITCHVAR",
            "DOT_AR_INITWALK_TIME",
            "DOT_AR_INITWALK_PITCHVAR",
            "BIT_AR_INTROREADTIMES",
            "BIT_AR_INTROREADTIME1",
            "BIT_AR_INTROREADTIME2",
            "DOT_AR_INTROREADTIMES",
            "DOT_AR_INTROREADTIME1",
            "DOT_AR_INTROREADTIME2",
            "BIT_MOTOR_TAPPING_DEVIATIONMEAN",
            "BIT_MOTOR_TAPPING_DEVIATIONVARIANCE",
            "BIT_MOTOR_TAPPING_NUMBEROFTAPS",
            "BIT_MOTOR_TAPPING_REACTIONTIMES",
            "BIT_MOTOR_TAPPING_TAPSPERSECOND",
            "BIT_MOTOR_TAPPING_NUMBEROFCORRECTTAPS",
            "BIT_MOTOR_TAPPING_DURATION",
            "BIT_MOTOR_TAPPING_FULLDURATION",
            "DOT_MOTOR_TAPPING_DEVIATIONMEAN",
            "DOT_MOTOR_TAPPING_DEVIATIONVARIANCE",
            "DOT_MOTOR_TAPPING_NUMBEROFTAPS",
            "DOT_MOTOR_TAPPING_REACTIONTIMES",
            "DOT_MOTOR_TAPPING_TAPSPERSECOND",
            "DOT_MOTOR_TAPPING_NUMBEROFCORRECTTAPS",
            "DOT_MOTOR_TAPPING_DURATION",
            "DOT_MOTOR_TAPPING_FULLDURATION",
            "BIT_MOTOR_EYE_CIRCLE_MOVEMENTSPEEDMEAN",
            "BIT_MOTOR_EYE_CIRCLE_MOVEMENTSPEEDVARIANCE",
            "BIT_MOTOR_EYE_CIRCLE_DISTANCEMEAN",
            "BIT_MOTOR_EYE_CIRCLE_DISTANCEVARIANCE",
            "BIT_MOTOR_EYE_CIRCLE_COVEIG1",
            "BIT_MOTOR_EYE_CIRCLE_COVEIG2",
            "BIT_MOTOR_EYE_SQUARE_MOVEMENTSPEEDMEAN",
            "BIT_MOTOR_EYE_SQUARE_MOVEMENTSPEEDVARIANCE",
            "BIT_MOTOR_EYE_SQUARE_DISTANCEMEAN",
            "BIT_MOTOR_EYE_SQUARE_DISTANCEVARIANCE",
            "BIT_MOTOR_EYE_SQUARE_COVEIG1",
            "BIT_MOTOR_EYE_SQUARE_COVEIG2",
            "BIT_MOTOR_EYE_SERPENTINE_MOVEMENTSPEEDMEAN",
            "BIT_MOTOR_EYE_SERPENTINE_MOVEMENTSPEEDVARIANCE",
            "BIT_MOTOR_EYE_SERPENTINE_DISTANCEMEAN",
            "BIT_MOTOR_EYE_SERPENTINE_DISTANCEVARIANCE",
            "BIT_MOTOR_EYE_SERPENTINE_COVEIG1",
            "BIT_MOTOR_EYE_SERPENTINE_COVEIG2",
            "BIT_MOTOR_EYE_SPEEDCIRCLE_MOVEMENTSPEEDMEAN",
            "BIT_MOTOR_EYE_SPEEDCIRCLE_MOVEMENTSPEEDVARIANCE",
            "BIT_MOTOR_EYE_SPEEDCIRCLE_DISTANCEMEAN",
            "BIT_MOTOR_EYE_SPEEDCIRCLE_DISTANCEVARIANCE",
            "BIT_MOTOR_EYE_SPEEDCIRCLE_COVEIG1",
            "BIT_MOTOR_EYE_SPEEDCIRCLE_COVEIG2",
            "BIT_MOTOR_EYE_TAPPING_MOVEMENTSPEEDMEAN",
            "BIT_MOTOR_EYE_TAPPING_MOVEMENTSPEEDVARIANCE",
            "BIT_MOTOR_EYE_TAPPING_DISTANCEMEAN",
            "BIT_MOTOR_EYE_TAPPING_DISTANCEVARIANCE",
            "BIT_MOTOR_EYE_TAPPING_COVEIG1",
            "BIT_MOTOR_EYE_TAPPING_COVEIG2",
            "BIT_MOTOR_EYE_RANDOMTAPPING_MOVEMENTSPEEDMEAN",
            "BIT_MOTOR_EYE_RANDOMTAPPING_MOVEMENTSPEEDVARIANCE",
            "BIT_MOTOR_EYE_RANDOMTAPPING_DISTANCEMEAN",
            "BIT_MOTOR_EYE_RANDOMTAPPING_DISTANCEVARIANCE",
            "BIT_MOTOR_EYE_RANDOMTAPPING_COVEIG1",
            "BIT_MOTOR_EYE_RANDOMTAPPING_COVEIG2",
            "DOT_MOTOR_EYE_CIRCLE_MOVEMENTSPEEDMEAN",
            "DOT_MOTOR_EYE_CIRCLE_MOVEMENTSPEEDVARIANCE",
            "DOT_MOTOR_EYE_CIRCLE_DISTANCEMEAN",
            "DOT_MOTOR_EYE_CIRCLE_DISTANCEVARIANCE",
            "DOT_MOTOR_EYE_CIRCLE_COVEIG1",
            "DOT_MOTOR_EYE_CIRCLE_COVEIG2",
            "DOT_MOTOR_EYE_SQUARE_MOVEMENTSPEEDMEAN",
            "DOT_MOTOR_EYE_SQUARE_MOVEMENTSPEEDVARIANCE",
            "DOT_MOTOR_EYE_SQUARE_DISTANCEMEAN",
            "DOT_MOTOR_EYE_SQUARE_DISTANCEVARIANCE",
            "DOT_MOTOR_EYE_SQUARE_COVEIG1",
            "DOT_MOTOR_EYE_SQUARE_COVEIG2",
            "DOT_MOTOR_EYE_SERPENTINE_MOVEMENTSPEEDMEAN",
            "DOT_MOTOR_EYE_SERPENTINE_MOVEMENTSPEEDVARIANCE",
            "DOT_MOTOR_EYE_SERPENTINE_DISTANCEMEAN",
            "DOT_MOTOR_EYE_SERPENTINE_DISTANCEVARIANCE",
            "DOT_MOTOR_EYE_SERPENTINE_COVEIG1",
            "DOT_MOTOR_EYE_SERPENTINE_COVEIG2",
            "DOT_MOTOR_EYE_SPEEDCIRCLE_MOVEMENTSPEEDMEAN",
            "DOT_MOTOR_EYE_SPEEDCIRCLE_MOVEMENTSPEEDVARIANCE",
            "DOT_MOTOR_EYE_SPEEDCIRCLE_DISTANCEMEAN",
            "DOT_MOTOR_EYE_SPEEDCIRCLE_DISTANCEVARIANCE",
            "DOT_MOTOR_EYE_SPEEDCIRCLE_COVEIG1",
            "DOT_MOTOR_EYE_SPEEDCIRCLE_COVEIG2",
            "DOT_MOTOR_EYE_TAPPING_MOVEMENTSPEEDMEAN",
            "DOT_MOTOR_EYE_TAPPING_MOVEMENTSPEEDVARIANCE",
            "DOT_MOTOR_EYE_TAPPING_DISTANCEMEAN",
            "DOT_MOTOR_EYE_TAPPING_DISTANCEVARIANCE",
            "DOT_MOTOR_EYE_TAPPING_COVEIG1",
            "DOT_MOTOR_EYE_TAPPING_COVEIG2",
            "DOT_MOTOR_EYE_RANDOMTAPPING_MOVEMENTSPEEDMEAN",
            "DOT_MOTOR_EYE_RANDOMTAPPING_MOVEMENTSPEEDVARIANCE",
            "DOT_MOTOR_EYE_RANDOMTAPPING_DISTANCEMEAN",
            "DOT_MOTOR_EYE_RANDOMTAPPING_DISTANCEVARIANCE",
            "DOT_MOTOR_EYE_RANDOMTAPPING_COVEIG1",
            "DOT_MOTOR_EYE_RANDOMTAPPING_COVEIG2",
            "BIT_SPEECH_PREDICTEDGENDER",
            "BIT_SPEECH_DOMINANTEMOTION",
            "BIT_SPEECH_DOMINANTEMOTION_FIRST5S",
            "BIT_SPEECH_DOMINANTEMOTION_MIDDLE5S",
            "BIT_SPEECH_DOMINANTEMOTION_LAST5S",
            "BIT_SPEECH_DOMINANTSOUNDTYPE",
            "BIT_SPEECH_DOMINANTSOUNDTYPE_FIRST5S",
            "BIT_SPEECH_DOMINANTSOUNDTYPE_MIDDLE5S",
            "BIT_SPEECH_DOMINANTSOUNDTYPE_LAST5S",
            "BIT_SPEECH_PERCENTAGESPEECH",
            "BIT_SPEECH_PERCENTAGESPEECH_FIRST5S",
            "BIT_SPEECH_PERCENTAGESPEECH_MIDDLE5S",
            "BIT_SPEECH_PERCENTAGESPEECH_LAST5S",
            "BIT_SPEECH_EMOTIONVARIANCE",
            "BIT_SPEECH_EMOTIONVARIANCE_FIRST5S",
            "BIT_SPEECH_EMOTIONVARIANCE_MIDDLE5S",
            "BIT_SPEECH_EMOTIONVARIANCE_LAST5S",
            "BIT_SPEECH_SOUNDTYPEVARIANCE",
            "BIT_SPEECH_SOUNDTYPEVARIANCE_FIRST5S",
            "BIT_SPEECH_SOUNDTYPEVARIANCE_MIDDLE5S",
            "BIT_SPEECH_SOUNDTYPEVARIANCE_LAST5S",
            "DOT_SPEECH_PREDICTEDGENDER",
            "DOT_SPEECH_DOMINANTEMOTION",
            "DOT_SPEECH_DOMINANTEMOTION_FIRST5S",
            "DOT_SPEECH_DOMINANTEMOTION_MIDDLE5S",
            "DOT_SPEECH_DOMINANTEMOTION_LAST5S",
            "DOT_SPEECH_DOMINANTSOUNDTYPE",
            "DOT_SPEECH_DOMINANTSOUNDTYPE_FIRST5S",
            "DOT_SPEECH_DOMINANTSOUNDTYPE_MIDDLE5S",
            "DOT_SPEECH_DOMINANTSOUNDTYPE_LAST5S",
            "DOT_SPEECH_PERCENTAGESPEECH",
            "DOT_SPEECH_PERCENTAGESPEECH_FIRST5S",
            "DOT_SPEECH_PERCENTAGESPEECH_MIDDLE5S",
            "DOT_SPEECH_PERCENTAGESPEECH_LAST5S",
            "DOT_SPEECH_EMOTIONVARIANCE",
            "DOT_SPEECH_EMOTIONVARIANCE_FIRST5S",
            "DOT_SPEECH_EMOTIONVARIANCE_MIDDLE5S",
            "DOT_SPEECH_EMOTIONVARIANCE_LAST5S",
            "DOT_SPEECH_SOUNDTYPEVARIANCE",
            "DOT_SPEECH_SOUNDTYPEVARIANCE_FIRST5S",
            "DOT_SPEECH_SOUNDTYPEVARIANCE_MIDDLE5S",
            "DOT_SPEECH_SOUNDTYPEVARIANCE_LAST5S",
            "BIT_AR_ANGULARCHANGEFINDOBJ1",
            "BIT_AR_ANGULARCHANGEFINDOBJ2",
            "BIT_AR_ANGULARCHANGEFINDOBJ3",
            "BIT_AR_PATHCOMPLEXITYFINDOBJ1",
            "BIT_AR_PATHCOMPLEXITYFINDOBJ2",
            "BIT_AR_PATHCOMPLEXITYFINDOBJ3",
            "DOT_AR_ANGULARCHANGEFINDOBJ1",
            "DOT_AR_ANGULARCHANGEFINDOBJ2",
            "DOT_AR_ANGULARCHANGEFINDOBJ3",
            "DOT_AR_PATHCOMPLEXITYFINDOBJ1",
            "DOT_AR_PATHCOMPLEXITYFINDOBJ2",
            "DOT_AR_PATHCOMPLEXITYFINDOBJ3",
            "BIT_AR_OBJECT1PLACEPITCH",
            "BIT_AR_OBJECT1PLACEROLL",
            "BIT_AR_OBJECT1PLACEYAW",
            "BIT_AR_OBJECT1FINDPITCH",
            "BIT_AR_OBJECT1FINDROLL",
            "BIT_AR_OBJECT1FINDYAW",
            "BIT_AR_OBJECT2PLACEPITCH",
            "BIT_AR_OBJECT2PLACEROLL",
            "BIT_AR_OBJECT2PLACEYAW",
            "BIT_AR_OBJECT2FINDPITCH",
            "BIT_AR_OBJECT2FINDROLL",
            "BIT_AR_OBJECT2FINDYAW",
            "BIT_AR_OBJECT3PLACEPITCH",
            "BIT_AR_OBJECT3PLACEROLL",
            "BIT_AR_OBJECT3PLACEYAW",
            "BIT_AR_OBJECT3FINDPITCH",
            "BIT_AR_OBJECT3FINDROLL",
            "BIT_AR_OBJECT3FINDYAW",
            "DOT_AR_OBJECT1PLACEPITCH",
            "DOT_AR_OBJECT1PLACEROLL",
            "DOT_AR_OBJECT1PLACEYAW",
            "DOT_AR_OBJECT1FINDPITCH",
            "DOT_AR_OBJECT1FINDROLL",
            "DOT_AR_OBJECT1FINDYAW",
            "DOT_AR_OBJECT2PLACEPITCH",
            "DOT_AR_OBJECT2PLACEROLL",
            "DOT_AR_OBJECT2PLACEYAW",
            "DOT_AR_OBJECT2FINDPITCH",
            "DOT_AR_OBJECT2FINDROLL",
            "DOT_AR_OBJECT2FINDYAW",
            "DOT_AR_OBJECT3PLACEPITCH",
            "DOT_AR_OBJECT3PLACEROLL",
            "DOT_AR_OBJECT3PLACEYAW",
            "DOT_AR_OBJECT3FINDPITCH",
            "DOT_AR_OBJECT3FINDROLL",
            "DOT_AR_OBJECT3FINDYAW",
            "BIT_AR_PLACEDELAYSAVERAGE",
            "BIT_AR_PLACEDELAYOBJ1",
            "BIT_AR_PLACEDELAYOBJ2",
            "BIT_AR_PLACEDELAYOBJ3",
            "DOT_AR_PLACEDELAYSAVERAGE",
            "DOT_AR_PLACEDELAYOBJ1",
            "DOT_AR_PLACEDELAYOBJ2",
            "DOT_AR_PLACEDELAYOBJ3",
            "BIT_AR_PLACEDURATIONSAVERAGE",
            "BIT_AR_PLACEDURATIONOBJ1",
            "BIT_AR_PLACEDURATIONOBJ2",
            "BIT_AR_PLACEDURATIONOBJ3",
            "DOT_AR_PLACEDURATIONSAVERAGE",
            "DOT_AR_PLACEDURATIONOBJ1",
            "DOT_AR_PLACEDURATIONOBJ2",
            "DOT_AR_PLACEDURATIONOBJ3",
            "BIT_AR_O1ACCXF0",
            "BIT_AR_O1ACCXF1",
            "BIT_AR_O1ACCXF2",
            "BIT_AR_O1ACCXF3",
            "BIT_AR_O1ACCXF4",
            "BIT_AR_O1ACCXF5",
            "BIT_AR_O1ACCXF6",
            "BIT_AR_O1ACCXF7",
            "BIT_AR_O1ACCXF8",
            "BIT_AR_O1ACCXF9",
            "BIT_AR_O1ACCYF0",
            "BIT_AR_O1ACCYF1",
            "BIT_AR_O1ACCYF2",
            "BIT_AR_O1ACCYF3",
            "BIT_AR_O1ACCYF4",
            "BIT_AR_O1ACCYF5",
            "BIT_AR_O1ACCYF6",
            "BIT_AR_O1ACCYF7",
            "BIT_AR_O1ACCYF8",
            "BIT_AR_O1ACCYF9",
            "BIT_AR_O1ACCZF0",
            "BIT_AR_O1ACCZF1",
            "BIT_AR_O1ACCZF2",
            "BIT_AR_O1ACCZF3",
            "BIT_AR_O1ACCZF4",
            "BIT_AR_O1ACCZF5",
            "BIT_AR_O1ACCZF6",
            "BIT_AR_O1ACCZF7",
            "BIT_AR_O1ACCZF8",
            "BIT_AR_O1ACCZF9",
            "BIT_AR_O1ATTXF0",
            "BIT_AR_O1ATTXF1",
            "BIT_AR_O1ATTXF2",
            "BIT_AR_O1ATTXF3",
            "BIT_AR_O1ATTXF4",
            "BIT_AR_O1ATTXF5",
            "BIT_AR_O1ATTXF6",
            "BIT_AR_O1ATTXF7",
            "BIT_AR_O1ATTXF8",
            "BIT_AR_O1ATTXF9",
            "BIT_AR_O1ATTYF0",
            "BIT_AR_O1ATTYF1",
            "BIT_AR_O1ATTYF2",
            "BIT_AR_O1ATTYF3",
            "BIT_AR_O1ATTYF4",
            "BIT_AR_O1ATTYF5",
            "BIT_AR_O1ATTYF6",
            "BIT_AR_O1ATTYF7",
            "BIT_AR_O1ATTYF8",
            "BIT_AR_O1ATTYF9",
            "BIT_AR_O1ATTZF0",
            "BIT_AR_O1ATTZF1",
            "BIT_AR_O1ATTZF2",
            "BIT_AR_O1ATTZF3",
            "BIT_AR_O1ATTZF4",
            "BIT_AR_O1ATTZF5",
            "BIT_AR_O1ATTZF6",
            "BIT_AR_O1ATTZF7",
            "BIT_AR_O1ATTZF8",
            "BIT_AR_O1ATTZF9",
            "BIT_AR_O2ACCXF0",
            "BIT_AR_O2ACCXF1",
            "BIT_AR_O2ACCXF2",
            "BIT_AR_O2ACCXF3",
            "BIT_AR_O2ACCXF4",
            "BIT_AR_O2ACCXF5",
            "BIT_AR_O2ACCXF6",
            "BIT_AR_O2ACCXF7",
            "BIT_AR_O2ACCXF8",
            "BIT_AR_O2ACCXF9",
            "BIT_AR_O2ACCYF0",
            "BIT_AR_O2ACCYF1",
            "BIT_AR_O2ACCYF2",
            "BIT_AR_O2ACCYF3",
            "BIT_AR_O2ACCYF4",
            "BIT_AR_O2ACCYF5",
            "BIT_AR_O2ACCYF6",
            "BIT_AR_O2ACCYF7",
            "BIT_AR_O2ACCYF8",
            "BIT_AR_O2ACCYF9",
            "BIT_AR_O2ACCZF0",
            "BIT_AR_O2ACCZF1",
            "BIT_AR_O2ACCZF2",
            "BIT_AR_O2ACCZF3",
            "BIT_AR_O2ACCZF4",
            "BIT_AR_O2ACCZF5",
            "BIT_AR_O2ACCZF6",
            "BIT_AR_O2ACCZF7",
            "BIT_AR_O2ACCZF8",
            "BIT_AR_O2ACCZF9",
            "BIT_AR_O2ATTXF0",
            "BIT_AR_O2ATTXF1",
            "BIT_AR_O2ATTXF2",
            "BIT_AR_O2ATTXF3",
            "BIT_AR_O2ATTXF4",
            "BIT_AR_O2ATTXF5",
            "BIT_AR_O2ATTXF6",
            "BIT_AR_O2ATTXF7",
            "BIT_AR_O2ATTXF8",
            "BIT_AR_O2ATTXF9",
            "BIT_AR_O2ATTYF0",
            "BIT_AR_O2ATTYF1",
            "BIT_AR_O2ATTYF2",
            "BIT_AR_O2ATTYF3",
            "BIT_AR_O2ATTYF4",
            "BIT_AR_O2ATTYF5",
            "BIT_AR_O2ATTYF6",
            "BIT_AR_O2ATTYF7",
            "BIT_AR_O2ATTYF8",
            "BIT_AR_O2ATTYF9",
            "BIT_AR_O2ATTZF0",
            "BIT_AR_O2ATTZF1",
            "BIT_AR_O2ATTZF2",
            "BIT_AR_O2ATTZF3",
            "BIT_AR_O2ATTZF4",
            "BIT_AR_O2ATTZF5",
            "BIT_AR_O2ATTZF6",
            "BIT_AR_O2ATTZF7",
            "BIT_AR_O2ATTZF8",
            "BIT_AR_O2ATTZF9",
            "BIT_AR_O3ACCXF0",
            "BIT_AR_O3ACCXF1",
            "BIT_AR_O3ACCXF2",
            "BIT_AR_O3ACCXF3",
            "BIT_AR_O3ACCXF4",
            "BIT_AR_O3ACCXF5",
            "BIT_AR_O3ACCXF6",
            "BIT_AR_O3ACCXF7",
            "BIT_AR_O3ACCXF8",
            "BIT_AR_O3ACCXF9",
            "BIT_AR_O3ACCYF0",
            "BIT_AR_O3ACCYF1",
            "BIT_AR_O3ACCYF2",
            "BIT_AR_O3ACCYF3",
            "BIT_AR_O3ACCYF4",
            "BIT_AR_O3ACCYF5",
            "BIT_AR_O3ACCYF6",
            "BIT_AR_O3ACCYF7",
            "BIT_AR_O3ACCYF8",
            "BIT_AR_O3ACCYF9",
            "BIT_AR_O3ACCZF0",
            "BIT_AR_O3ACCZF1",
            "BIT_AR_O3ACCZF2",
            "BIT_AR_O3ACCZF3",
            "BIT_AR_O3ACCZF4",
            "BIT_AR_O3ACCZF5",
            "BIT_AR_O3ACCZF6",
            "BIT_AR_O3ACCZF7",
            "BIT_AR_O3ACCZF8",
            "BIT_AR_O3ACCZF9",
            "BIT_AR_O3ATTXF0",
            "BIT_AR_O3ATTXF1",
            "BIT_AR_O3ATTXF2",
            "BIT_AR_O3ATTXF3",
            "BIT_AR_O3ATTXF4",
            "BIT_AR_O3ATTXF5",
            "BIT_AR_O3ATTXF6",
            "BIT_AR_O3ATTXF7",
            "BIT_AR_O3ATTXF8",
            "BIT_AR_O3ATTXF9",
            "BIT_AR_O3ATTYF0",
            "BIT_AR_O3ATTYF1",
            "BIT_AR_O3ATTYF2",
            "BIT_AR_O3ATTYF3",
            "BIT_AR_O3ATTYF4",
            "BIT_AR_O3ATTYF5",
            "BIT_AR_O3ATTYF6",
            "BIT_AR_O3ATTYF7",
            "BIT_AR_O3ATTYF8",
            "BIT_AR_O3ATTYF9",
            "BIT_AR_O3ATTZF0",
            "BIT_AR_O3ATTZF1",
            "BIT_AR_O3ATTZF2",
            "BIT_AR_O3ATTZF3",
            "BIT_AR_O3ATTZF4",
            "BIT_AR_O3ATTZF5",
            "BIT_AR_O3ATTZF6",
            "BIT_AR_O3ATTZF7",
            "BIT_AR_O3ATTZF8",
            "BIT_AR_O3ATTZF9",
            "DOT_AR_O1ACCXF0",
            "DOT_AR_O1ACCXF1",
            "DOT_AR_O1ACCXF2",
            "DOT_AR_O1ACCXF3",
            "DOT_AR_O1ACCXF4",
            "DOT_AR_O1ACCXF5",
            "DOT_AR_O1ACCXF6",
            "DOT_AR_O1ACCXF7",
            "DOT_AR_O1ACCXF8",
            "DOT_AR_O1ACCXF9",
            "DOT_AR_O1ACCYF0",
            "DOT_AR_O1ACCYF1",
            "DOT_AR_O1ACCYF2",
            "DOT_AR_O1ACCYF3",
            "DOT_AR_O1ACCYF4",
            "DOT_AR_O1ACCYF5",
            "DOT_AR_O1ACCYF6",
            "DOT_AR_O1ACCYF7",
            "DOT_AR_O1ACCYF8",
            "DOT_AR_O1ACCYF9",
            "DOT_AR_O1ACCZF0",
            "DOT_AR_O1ACCZF1",
            "DOT_AR_O1ACCZF2",
            "DOT_AR_O1ACCZF3",
            "DOT_AR_O1ACCZF4",
            "DOT_AR_O1ACCZF5",
            "DOT_AR_O1ACCZF6",
            "DOT_AR_O1ACCZF7",
            "DOT_AR_O1ACCZF8",
            "DOT_AR_O1ACCZF9",
            "DOT_AR_O1ATTXF0",
            "DOT_AR_O1ATTXF1",
            "DOT_AR_O1ATTXF2",
            "DOT_AR_O1ATTXF3",
            "DOT_AR_O1ATTXF4",
            "DOT_AR_O1ATTXF5",
            "DOT_AR_O1ATTXF6",
            "DOT_AR_O1ATTXF7",
            "DOT_AR_O1ATTXF8",
            "DOT_AR_O1ATTXF9",
            "DOT_AR_O1ATTYF0",
            "DOT_AR_O1ATTYF1",
            "DOT_AR_O1ATTYF2",
            "DOT_AR_O1ATTYF3",
            "DOT_AR_O1ATTYF4",
            "DOT_AR_O1ATTYF5",
            "DOT_AR_O1ATTYF6",
            "DOT_AR_O1ATTYF7",
            "DOT_AR_O1ATTYF8",
            "DOT_AR_O1ATTYF9",
            "DOT_AR_O1ATTZF0",
            "DOT_AR_O1ATTZF1",
            "DOT_AR_O1ATTZF2",
            "DOT_AR_O1ATTZF3",
            "DOT_AR_O1ATTZF4",
            "DOT_AR_O1ATTZF5",
            "DOT_AR_O1ATTZF6",
            "DOT_AR_O1ATTZF7",
            "DOT_AR_O1ATTZF8",
            "DOT_AR_O1ATTZF9",
            "DOT_AR_O2ACCXF0",
            "DOT_AR_O2ACCXF1",
            "DOT_AR_O2ACCXF2",
            "DOT_AR_O2ACCXF3",
            "DOT_AR_O2ACCXF4",
            "DOT_AR_O2ACCXF5",
            "DOT_AR_O2ACCXF6",
            "DOT_AR_O2ACCXF7",
            "DOT_AR_O2ACCXF8",
            "DOT_AR_O2ACCXF9",
            "DOT_AR_O2ACCYF0",
            "DOT_AR_O2ACCYF1",
            "DOT_AR_O2ACCYF2",
            "DOT_AR_O2ACCYF3",
            "DOT_AR_O2ACCYF4",
            "DOT_AR_O2ACCYF5",
            "DOT_AR_O2ACCYF6",
            "DOT_AR_O2ACCYF7",
            "DOT_AR_O2ACCYF8",
            "DOT_AR_O2ACCYF9",
            "DOT_AR_O2ACCZF0",
            "DOT_AR_O2ACCZF1",
            "DOT_AR_O2ACCZF2",
            "DOT_AR_O2ACCZF3",
            "DOT_AR_O2ACCZF4",
            "DOT_AR_O2ACCZF5",
            "DOT_AR_O2ACCZF6",
            "DOT_AR_O2ACCZF7",
            "DOT_AR_O2ACCZF8",
            "DOT_AR_O2ACCZF9",
            "DOT_AR_O2ATTXF0",
            "DOT_AR_O2ATTXF1",
            "DOT_AR_O2ATTXF2",
            "DOT_AR_O2ATTXF3",
            "DOT_AR_O2ATTXF4",
            "DOT_AR_O2ATTXF5",
            "DOT_AR_O2ATTXF6",
            "DOT_AR_O2ATTXF7",
            "DOT_AR_O2ATTXF8",
            "DOT_AR_O2ATTXF9",
            "DOT_AR_O2ATTYF0",
            "DOT_AR_O2ATTYF1",
            "DOT_AR_O2ATTYF2",
            "DOT_AR_O2ATTYF3",
            "DOT_AR_O2ATTYF4",
            "DOT_AR_O2ATTYF5",
            "DOT_AR_O2ATTYF6",
            "DOT_AR_O2ATTYF7",
            "DOT_AR_O2ATTYF8",
            "DOT_AR_O2ATTYF9",
            "DOT_AR_O2ATTZF0",
            "DOT_AR_O2ATTZF1",
            "DOT_AR_O2ATTZF2",
            "DOT_AR_O2ATTZF3",
            "DOT_AR_O2ATTZF4",
            "DOT_AR_O2ATTZF5",
            "DOT_AR_O2ATTZF6",
            "DOT_AR_O2ATTZF7",
            "DOT_AR_O2ATTZF8",
            "DOT_AR_O2ATTZF9",
            "DOT_AR_O3ACCXF0",
            "DOT_AR_O3ACCXF1",
            "DOT_AR_O3ACCXF2",
            "DOT_AR_O3ACCXF3",
            "DOT_AR_O3ACCXF4",
            "DOT_AR_O3ACCXF5",
            "DOT_AR_O3ACCXF6",
            "DOT_AR_O3ACCXF7",
            "DOT_AR_O3ACCXF8",
            "DOT_AR_O3ACCXF9",
            "DOT_AR_O3ACCYF0",
            "DOT_AR_O3ACCYF1",
            "DOT_AR_O3ACCYF2",
            "DOT_AR_O3ACCYF3",
            "DOT_AR_O3ACCYF4",
            "DOT_AR_O3ACCYF5",
            "DOT_AR_O3ACCYF6",
            "DOT_AR_O3ACCYF7",
            "DOT_AR_O3ACCYF8",
            "DOT_AR_O3ACCYF9",
            "DOT_AR_O3ACCZF0",
            "DOT_AR_O3ACCZF1",
            "DOT_AR_O3ACCZF2",
            "DOT_AR_O3ACCZF3",
            "DOT_AR_O3ACCZF4",
            "DOT_AR_O3ACCZF5",
            "DOT_AR_O3ACCZF6",
            "DOT_AR_O3ACCZF7",
            "DOT_AR_O3ACCZF8",
            "DOT_AR_O3ACCZF9",
            "DOT_AR_O3ATTXF0",
            "DOT_AR_O3ATTXF1",
            "DOT_AR_O3ATTXF2",
            "DOT_AR_O3ATTXF3",
            "DOT_AR_O3ATTXF4",
            "DOT_AR_O3ATTXF5",
            "DOT_AR_O3ATTXF6",
            "DOT_AR_O3ATTXF7",
            "DOT_AR_O3ATTXF8",
            "DOT_AR_O3ATTXF9",
            "DOT_AR_O3ATTYF0",
            "DOT_AR_O3ATTYF1",
            "DOT_AR_O3ATTYF2",
            "DOT_AR_O3ATTYF3",
            "DOT_AR_O3ATTYF4",
            "DOT_AR_O3ATTYF5",
            "DOT_AR_O3ATTYF6",
            "DOT_AR_O3ATTYF7",
            "DOT_AR_O3ATTYF8",
            "DOT_AR_O3ATTYF9",
            "DOT_AR_O3ATTZF0",
            "DOT_AR_O3ATTZF1",
            "DOT_AR_O3ATTZF2",
            "DOT_AR_O3ATTZF3",
            "DOT_AR_O3ATTZF4",
            "DOT_AR_O3ATTZF5",
            "DOT_AR_O3ATTZF6",
            "DOT_AR_O3ATTZF7",
            "DOT_AR_O3ATTZF8",
            "DOT_AR_O3ATTZF9",
            "BIT_MOTOR_RANDOMTAPPING_DEVIATIONMEAN",
            "BIT_MOTOR_RANDOMTAPPING_DEVIATIONVARIANCE",
            "BIT_MOTOR_RANDOMTAPPING_NUMBEROFTAPS",
            "BIT_MOTOR_RANDOMTAPPING_REACTIONTIMES",
            "BIT_MOTOR_RANDOMTAPPING_TAPSPERSECOND",
            "BIT_MOTOR_RANDOMTAPPING_NUMBEROFCORRECTTAPS",
            "BIT_MOTOR_RANDOMTAPPING_DURATION",
            "BIT_MOTOR_RANDOMTAPPING_FULLDURATION",
            "DOT_MOTOR_RANDOMTAPPING_DEVIATIONMEAN",
            "DOT_MOTOR_RANDOMTAPPING_DEVIATIONVARIANCE",
            "DOT_MOTOR_RANDOMTAPPING_NUMBEROFTAPS",
            "DOT_MOTOR_RANDOMTAPPING_REACTIONTIMES",
            "DOT_MOTOR_RANDOMTAPPING_TAPSPERSECOND",
            "DOT_MOTOR_RANDOMTAPPING_NUMBEROFCORRECTTAPS",
            "DOT_MOTOR_RANDOMTAPPING_DURATION",
            "DOT_MOTOR_RANDOMTAPPING_FULLDURATION",
            "BIT_MOTOR_SERPENTINE_DEVIATIONMEAN",
            "BIT_MOTOR_SERPENTINE_DEVIATIONVARIANCE",
            "BIT_MOTOR_SERPENTINE_TIMERATIOWITHIN",
            "BIT_MOTOR_SERPENTINE_DISTRATIOWITHIN",
            "BIT_MOTOR_SERPENTINE_DISTWITHIN",
            "BIT_MOTOR_SERPENTINE_DISTTOTAL",
            "BIT_MOTOR_SERPENTINE_YPROGRESS",
            "BIT_MOTOR_SERPENTINE_SPEED",
            "BIT_MOTOR_SERPENTINE_SPEEDACCURACYRATIO",
            "BIT_MOTOR_SERPENTINE_DURATION",
            "BIT_MOTOR_SERPENTINE_FULLDURATION",
            "DOT_MOTOR_SERPENTINE_DEVIATIONMEAN",
            "DOT_MOTOR_SERPENTINE_DEVIATIONVARIANCE",
            "DOT_MOTOR_SERPENTINE_TIMERATIOWITHIN",
            "DOT_MOTOR_SERPENTINE_DISTRATIOWITHIN",
            "DOT_MOTOR_SERPENTINE_DISTWITHIN",
            "DOT_MOTOR_SERPENTINE_DISTTOTAL",
            "DOT_MOTOR_SERPENTINE_YPROGRESS",
            "DOT_MOTOR_SERPENTINE_SPEED",
            "DOT_MOTOR_SERPENTINE_SPEEDACCURACYRATIO",
            "DOT_MOTOR_SERPENTINE_DURATION",
            "DOT_MOTOR_SERPENTINE_FULLDURATION",
            "BIT_MOTOR_PRESSURE_CIRCLE_PRESSUREMEAN",
            "BIT_MOTOR_PRESSURE_CIRCLE_PRESSUREVARIANCE",
            "BIT_MOTOR_PRESSURE_SQUARE_PRESSUREMEAN",
            "BIT_MOTOR_PRESSURE_SQUARE_PRESSUREVARIANCE",
            "BIT_MOTOR_PRESSURE_SERPENTINE_PRESSUREMEAN",
            "BIT_MOTOR_PRESSURE_SERPENTINE_PRESSUREVARIANCE",
            "BIT_MOTOR_PRESSURE_SPEEDCIRCLE_PRESSUREMEAN",
            "BIT_MOTOR_PRESSURE_SPEEDCIRCLE_PRESSUREVARIANCE",
            "BIT_MOTOR_PRESSURE_TAPPING_PRESSUREMEAN",
            "BIT_MOTOR_PRESSURE_TAPPING_PRESSUREVARIANCE",
            "BIT_MOTOR_PRESSURE_RANDOMTAPPING_PRESSUREMEAN",
            "BIT_MOTOR_PRESSURE_RANDOMTAPPING_PRESSUREVARIANCE",
            "DOT_MOTOR_PRESSURE_CIRCLE_PRESSUREMEAN",
            "DOT_MOTOR_PRESSURE_CIRCLE_PRESSUREVARIANCE",
            "DOT_MOTOR_PRESSURE_SQUARE_PRESSUREMEAN",
            "DOT_MOTOR_PRESSURE_SQUARE_PRESSUREVARIANCE",
            "DOT_MOTOR_PRESSURE_SERPENTINE_PRESSUREMEAN",
            "DOT_MOTOR_PRESSURE_SERPENTINE_PRESSUREVARIANCE",
            "DOT_MOTOR_PRESSURE_SPEEDCIRCLE_PRESSUREMEAN",
            "DOT_MOTOR_PRESSURE_SPEEDCIRCLE_PRESSUREVARIANCE",
            "DOT_MOTOR_PRESSURE_TAPPING_PRESSUREMEAN",
            "DOT_MOTOR_PRESSURE_TAPPING_PRESSUREVARIANCE",
            "DOT_MOTOR_PRESSURE_RANDOMTAPPING_PRESSUREMEAN",
            "DOT_MOTOR_PRESSURE_RANDOMTAPPING_PRESSUREVARIANCE",
            "BIT_AR_SKIPBUTTONCOUNT",
            "DOT_AR_SKIPBUTTONCOUNT",
            "BIT_MOTOR_SPEEDCIRCLE_DEVIATIONMEAN",
            "BIT_MOTOR_SPEEDCIRCLE_DEVIATIONVARIANCE",
            "BIT_MOTOR_SPEEDCIRCLE_TIMERATIOWITHIN",
            "BIT_MOTOR_SPEEDCIRCLE_DISTRATIOWITHIN",
            "BIT_MOTOR_SPEEDCIRCLE_DISTWITHIN",
            "BIT_MOTOR_SPEEDCIRCLE_DISTTOTAL",
            "BIT_MOTOR_SPEEDCIRCLE_ANGLESUM",
            "BIT_MOTOR_SPEEDCIRCLE_SPEED",
            "BIT_MOTOR_SPEEDCIRCLE_SPEEDACCURACYRATIO",
            "BIT_MOTOR_SPEEDCIRCLE_DURATION",
            "BIT_MOTOR_SPEEDCIRCLE_FULLDURATION",
            "DOT_MOTOR_SPEEDCIRCLE_DEVIATIONMEAN",
            "DOT_MOTOR_SPEEDCIRCLE_DEVIATIONVARIANCE",
            "DOT_MOTOR_SPEEDCIRCLE_TIMERATIOWITHIN",
            "DOT_MOTOR_SPEEDCIRCLE_DISTRATIOWITHIN",
            "DOT_MOTOR_SPEEDCIRCLE_DISTWITHIN",
            "DOT_MOTOR_SPEEDCIRCLE_DISTTOTAL",
            "DOT_MOTOR_SPEEDCIRCLE_ANGLESUM",
            "DOT_MOTOR_SPEEDCIRCLE_SPEED",
            "DOT_MOTOR_SPEEDCIRCLE_SPEEDACCURACYRATIO",
            "DOT_MOTOR_SPEEDCIRCLE_DURATION",
            "DOT_MOTOR_SPEEDCIRCLE_FULLDURATION",
            "BIT_AR_SPOTALREADYTAKENCOUNT",
            "DOT_AR_SPOTALREADYTAKENCOUNT",
            "BIT_MOTOR_SQUARE_DEVIATIONMEAN",
            "BIT_MOTOR_SQUARE_DEVIATIONVARIANCE",
            "BIT_MOTOR_SQUARE_TIMERATIOWITHIN",
            "BIT_MOTOR_SQUARE_DISTRATIOWITHIN",
            "BIT_MOTOR_SQUARE_DISTWITHIN",
            "BIT_MOTOR_SQUARE_DISTTOTAL",
            "BIT_MOTOR_SQUARE_ANGLESUM",
            "BIT_MOTOR_SQUARE_SPEED",
            "BIT_MOTOR_SQUARE_SPEEDACCURACYRATIO",
            "BIT_MOTOR_SQUARE_DURATION",
            "BIT_MOTOR_SQUARE_FULLDURATION",
            "DOT_MOTOR_SQUARE_DEVIATIONMEAN",
            "DOT_MOTOR_SQUARE_DEVIATIONVARIANCE",
            "DOT_MOTOR_SQUARE_TIMERATIOWITHIN",
            "DOT_MOTOR_SQUARE_DISTRATIOWITHIN",
            "DOT_MOTOR_SQUARE_DISTWITHIN",
            "DOT_MOTOR_SQUARE_DISTTOTAL",
            "DOT_MOTOR_SQUARE_ANGLESUM",
            "DOT_MOTOR_SQUARE_SPEED",
            "DOT_MOTOR_SQUARE_SPEEDACCURACYRATIO",
            "DOT_MOTOR_SQUARE_DURATION",
            "DOT_MOTOR_SQUARE_FULLDURATION",
            "BITDOT_MOTOR_CIRCLE_RATIOSTARTTIME",
            "BITDOT_MOTOR_SQUARE_RATIOSTARTTIME",
            "BITDOT_MOTOR_SERPENTINE_RATIOSTARTTIME",
            "BITDOT_MOTOR_SPEEDCIRCLE_RATIOSTARTTIME",
            "BITDOT_MOTOR_TAPPING_RATIOSTARTTIME",
            "BITDOT_MOTOR_RANDOMTAPPING_RATIOSTARTTIME",
            "BIT_AR_STEPCOUNTFINDPLACERATIO",
            "BIT_AR_PLACINGMEANSTEPDELAY",
            "BIT_AR_PLACINGSTEPVARIANCE",
            "BIT_AR_PLACINGNOTWALKINGTIME",
            "BIT_AR_FINDINGMEANSTEPDELAY",
            "BIT_AR_FINDINGSTEPVARIANCE",
            "BIT_AR_FINDINGNOTWALKINGTIME",
            "BIT_AR_WALKTURNANALYSISFINDOBJ1",
            "BIT_AR_WALKTURNANALYSISFINDOBJ2",
            "BIT_AR_WALKTURNANALYSISFINDOBJ3",
            "BIT_AR_WALKTURNANALYSISFINDAVG",
            "DOT_AR_STEPCOUNTFINDPLACERATIO",
            "DOT_AR_PLACINGMEANSTEPDELAY",
            "DOT_AR_PLACINGSTEPVARIANCE",
            "DOT_AR_PLACINGNOTWALKINGTIME",
            "DOT_AR_FINDINGMEANSTEPDELAY",
            "DOT_AR_FINDINGSTEPVARIANCE",
            "DOT_AR_FINDINGNOTWALKINGTIME",
            "DOT_AR_WALKTURNANALYSISFINDOBJ1",
            "DOT_AR_WALKTURNANALYSISFINDOBJ2",
            "DOT_AR_WALKTURNANALYSISFINDOBJ3",
            "DOT_AR_WALKTURNANALYSISFINDAVG",
            "TIMEBETWEENBITDOT",
            "BIT_AR_TOOMUCHMOVEMENTCOUNT",
            "DOT_AR_TOOMUCHMOVEMENTCOUNT",
            "YEARSOFEDUCATION",
        )
    }
}
