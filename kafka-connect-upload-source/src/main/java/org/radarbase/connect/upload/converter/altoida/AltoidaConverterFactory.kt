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
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.FilePreProcessorFactory
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarbase.connect.upload.converter.altoida.summary.AltoidaDomainResultProcessor
import org.radarbase.connect.upload.converter.altoida.summary.AltoidaSummaryCsvPreProcessorFactory
import org.radarbase.connect.upload.converter.altoida.summary.AltoidaSummaryProcessor
import org.radarbase.connect.upload.converter.altoida.summary.AltoidaTestMetricsProcessor
import org.radarbase.connect.upload.converter.archive.ArchiveProcessorFactory.Companion.zipFactory
import org.radarbase.connect.upload.converter.csv.CsvFileProcessorFactory
import org.radarbase.connect.upload.logging.LogRepository

class AltoidaConverterFactory : ConverterFactory {
    override val sourceType: String = "altoida"

    override fun filePreProcessorFactories(
        settings: Map<String, String>,
        connectorConfig: SourceTypeDTO,
        logRepository: LogRepository
    ): List<FilePreProcessorFactory> = listOf(
        // Preprocess malformed export.csv
        AltoidaSummaryCsvPreProcessorFactory(),
    )

    override fun fileProcessorFactories(
            settings: Map<String, String>,
            connectorConfig: SourceTypeDTO,
            logRepository: LogRepository
    ): List<FileProcessorFactory> = listOf(
        // Process export.csv
        CsvFileProcessorFactory(
            csvProcessorFactories = listOf(
                AltoidaSummaryProcessor(),
                AltoidaDomainResultProcessor(),
                AltoidaTestMetricsProcessor(AltoidaTestMetricsProcessor.AltoidaTestCategory.BIT, "connect_upload_altoida_bit_metrics"),
                AltoidaTestMetricsProcessor(AltoidaTestMetricsProcessor.AltoidaTestCategory.DOT, "connect_upload_altoida_dot_metrics"),
            ),
        ),
        // Process zip file with detailed CSV contents
        zipFactory(
            sourceType,
            entryProcessors = listOf(
                CsvFileProcessorFactory(
                    csvProcessorFactories = listOf(
                        AltoidaAccelerationCsvProcessor(),
                        AltoidaActionCsvProcessor(),
                        AltoidaAttitudeCsvProcessor(),
                        AltoidaBlinkCsvProcessor(),
                        AltoidaDiagnosticsCsvProcessor(),
                        AltoidaEyeTrackingCsvProcessor(),
                        AltoidaGravityCsvProcessor(),
                        AltoidaMagneticFieldCsvProcessor(),
                        AltoidaObjectCsvProcessor(),
                        AltoidaPathCsvProcessor(),
                        AltoidaRotationCsvProcessor(),
                        AltoidaTapScreenCsvProcessor(),
                        AltoidaTouchScreenCsvProcessor(),
                    )
                ),
                AltoidaMetadataFileProcessor(),
            ),
            allowUnmappedFiles = true,
        ),
    )
}

