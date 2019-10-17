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

package org.radarbase.connect.upload.converter.altoida

import org.radarbase.connect.upload.converter.AbstractCsvProcessor
import org.radarbase.connect.upload.converter.CsvFileRecordConverter
import org.radarbase.connect.upload.converter.CsvProcessor
import org.radarbase.connect.upload.converter.TopicData
import org.radarcns.connector.upload.altoida.AltoidaTouchscreen

class AltoidaTouchscreenCsvProcessor(
        override val schemaType: String = "_NORMAL.csv",
        val topic: String = "connect_upload_altoida_touchscreen") : AbstractCsvProcessor(schemaType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "X", "Y") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val normalVector = AltoidaTouchscreen(
                time,
                timeReceived,
                lineValues["X"]?.toDouble(),
                lineValues["Y"]?.toDouble()
        )

        return TopicData(false, topic, normalVector)
    }
}

class AltoidaTouchscreenConverter(
        sourceType: String = "altoida_touchscreen", csvProcessor: CsvProcessor = AltoidaTouchscreenCsvProcessor())
    : CsvFileRecordConverter(sourceType, csvProcessor)
