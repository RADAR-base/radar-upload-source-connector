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

import org.radarbase.connect.upload.converter.*
import org.radarcns.connector.upload.altoida.AltoidaObject

class AltoidaObjectCsvProcessor(
        override val schemaType: String = "_OBJECTS.csv",
        val topic: String = "connect_upload_altoida_object") : AbstractCsvProcessor(schemaType) {
    
    override fun validateHeaderSchema(csvHeader: List<String>) =
            listOf("TIMESTAMP", "OBJ", "X", "Y", "Z") == (csvHeader)

    override fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double): TopicData? {
        val time = lineValues["TIMESTAMP"]?.toDouble()
        val objects = AltoidaObject(
                time,
                timeReceived,
                lineValues["OBJ"],
                lineValues["X"]?.toFloat(),
                lineValues["Y"]?.toFloat(),
                lineValues["Z"]?.toFloat()
                )

        return TopicData(false, topic, objects)
    }
}

class AltoidaObjectConverter(
        sourceType: String = "altoida_object", csvProcessor: CsvProcessor = AltoidaObjectCsvProcessor())
    : CsvFileRecordConverter(sourceType, csvProcessor)
