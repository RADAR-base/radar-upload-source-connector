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

import org.radarbase.connect.upload.api.SourceTypeDTO
import org.radarbase.connect.upload.converter.*

class AltoidaZipConverterFactory : ConverterFactory {
    override val sourceType: String = "altoida-zip"

    override fun fileProcessorFactories(
            settings: Map<String, String>,
            connectorConfig: SourceTypeDTO,
            logRepository: LogRepository
    ): List<FileProcessorFactory> {
        val csvLineProcessors  = listOf(
                AltoidaAccelerationCsvProcessor(),
                AltoidaActionCsvProcessor(),
                AltoidaAttitudeCsvProcessor(),
                AltoidaDiagnosticsCsvProcessor(),
                AltoidaGravityCsvProcessor(),
                AltoidaMagnetometerCsvProcessor(),
                AltoidaObjectCsvProcessor(),
                AltoidaPathCsvProcessor(),
                AltoidaRotationCsvProcessor(),
                AltoidaTouchscreenCsvProcessor())

        val fileProcessors = listOf(
                CsvProcessorFactory(csvLineProcessors, logRepository),
                AltoidaMetadataFileProcessor(logRepository))

        return listOf(ZipFileProcessorFactory(fileProcessors, logRepository))
    }
}

