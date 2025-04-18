/*
 *  Copyright 2019 The Hyve
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.radarbase.connect.upload.converter

import org.radarbase.connect.upload.converter.csv.CsvLineProcessorFactory

/**
 * Simple Processor for one line to one record of a single topic conversion.
 */
abstract class StatelessCsvLineProcessor : CsvLineProcessorFactory {
    override val fileNameSuffixes: List<String>
        get() = listOf(fileNameSuffix)

    open val fileNameSuffix: String = ".csv"

    override val headerMustMatch: Boolean
        get() = fileNameSuffixes != listOf(".csv")

    override val optional: Boolean = false

    open val timeFieldParser: TimeFieldParser = TimeFieldParser.EpochMillisParser()

    fun time(line: Map<String, String>): Double = timeFieldParser.time(line)

    open fun lineConversion(
        line: Map<String, String>,
        timeReceived: Double,
    ): TopicData? = null

    open fun lineConversions(
        line: Map<String, String>,
        timeReceived: Double,
    ): Sequence<TopicData> {
        val conversion = lineConversion(line, timeReceived)
        return if (conversion != null) sequenceOf(conversion) else emptySequence()
    }

    override fun createLineProcessor(
        context: ConverterFactory.ContentsContext,
    ): CsvLineProcessorFactory.CsvLineProcessor {
        return Processor(context) { l, t -> lineConversions(l, t) }
    }

    internal class Processor(
        override val context: ConverterFactory.ContentsContext,
        private val conversion: Processor.(lineValues: Map<String, String>, timeReceived: Double) -> Sequence<TopicData>,
    ) : CsvLineProcessorFactory.CsvLineProcessor {
        override fun convertToRecord(
            lineValues: Map<String, String>,
            timeReceived: Double,
        ): Sequence<TopicData> = conversion(lineValues, timeReceived)
    }
}
