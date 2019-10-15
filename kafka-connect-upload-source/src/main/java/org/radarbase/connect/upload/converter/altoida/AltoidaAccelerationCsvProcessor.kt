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

import org.apache.avro.generic.IndexedRecord
import org.radarbase.connect.upload.converter.SimpleCsvLineProcessor
import org.radarcns.connector.upload.altoida.AltoidaAcceleration

class AltoidaAccelerationCsvProcessor : AltoidaCsvProcessor() {
    override val fileNameSuffix: String = "_ACC.csv"

    override val topic: String = "connect_upload_altoida_acceleration"

    override val header: List<String> = listOf("TIMESTAMP", "X", "Y", "Z")

    override fun SimpleCsvLineProcessor.lineConversion(
            line: Map<String, String>,
            timeReceived: Double
    ): IndexedRecord = AltoidaAcceleration(
            line["TIMESTAMP"]?.toDouble(),
            timeReceived,
            line["X"]?.toFloat(),
            line["Y"]?.toFloat(),
            line["Z"]?.toFloat())

}
