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

import org.radarbase.connect.upload.converter.StatelessCsvLineProcessor
import org.radarbase.connect.upload.converter.TopicData
import org.radarcns.connector.upload.altoida.AltoidaEyeTracking

class AltoidaEyeTrackingCsvProcessor : StatelessCsvLineProcessor() {
    override val fileNameSuffix: String = "eye_screen_focuspoint.csv"

    override val header: List<String> = listOf("TIMESTAMP", "X", "Y")

    override fun lineConversion(
            line: Map<String, String>,
            timeReceived: Double
    ) = TopicData("connect_upload_altoida_eye_tracking", AltoidaEyeTracking(
            time(line),
            timeReceived,
            line.getValue("X").toFloatOrNull(),
            line.getValue("Y").toFloatOrNull()))
}
