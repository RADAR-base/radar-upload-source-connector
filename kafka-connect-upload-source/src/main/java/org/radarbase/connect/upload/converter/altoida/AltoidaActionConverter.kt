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
import org.radarcns.connector.upload.altoida.AltoidaAction

class AltoidaActionCsvProcessor(
        override val schemaType: String = "_TAG.csv",
        val topic: String = "connect_upload_altoida_action") : AbstractCsvProcessor(schemaType) {

    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "TAG", "PAYLOAD") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val log = AltoidaAction(
                time,
                timeReceived,
                lineValues["TAG"],
                lineValues["PAYLOAD"]
        )

        return TopicData(false, topic, log)
    }
}

class AltoidaActionConverter(
        sourceType: String = "altoida_action", csvProcessor: CsvProcessor = AltoidaActionCsvProcessor())
    : CsvFileRecordConverter(sourceType, csvProcessor)
