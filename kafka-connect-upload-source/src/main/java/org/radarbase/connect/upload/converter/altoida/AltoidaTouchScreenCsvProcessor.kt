/*
 * Copyright 2019 The Hyve
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.radarbase.connect.upload.converter.altoida

import org.radarbase.connect.upload.converter.OneToOneCsvLineProcessor
import org.radarcns.connector.upload.altoida.AltoidaTouch

class AltoidaTouchScreenCsvProcessor : AltoidaCsvProcessor() {
    override val fileNameSuffix: String = "_TOUCH.csv"

    override val topic: String = "connect_upload_altoida_touch"

    override val header: List<String> = listOf("TIMESTAMP", "X", "Y", "SURFACE", "ACC", "COMBINED", "HIT")

    override fun OneToOneCsvLineProcessor.lineConversion(
            line: Map<String, String>,
            timeReceived: Double
    ) = AltoidaTouch(
            time(line),
            timeReceived,
            line.getValue("X").toDouble(),
            line.getValue("Y").toDouble(),
            line.getValue("SURFACE").toDouble(),
            line.getValue("ACC").toDouble(),
            line.getValue("COMBINED").toDouble(),
            line.getValue("HIT").toBoolean())
}
