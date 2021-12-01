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

package org.radarbase.connect.upload.converter.altoida_v2

import org.radarbase.connect.upload.api.SourceTypeDTO
import org.radarbase.connect.upload.converter.*
import org.radarbase.connect.upload.converter.altoida.summary.*
import org.radarbase.connect.upload.converter.csv.CsvFileProcessorFactory
import org.radarbase.connect.upload.converter.xml.XmlFileProcessorFactory
import org.radarbase.connect.upload.converter.zip.ZipFileProcessorFactory
import org.radarbase.connect.upload.logging.LogRepository

class AltoidaConverterFactory : ConverterFactory {
    override val sourceType: String = "altoida_v2"

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
            // Process xml file
            XmlFileProcessorFactory(
                    xmlProcessorFactories = listOf(
//                            AltoidaAssessmentsSummaryXmlProcessor(),
//                            AltoidaContrastVisionTestXmlProcessor(),
//                            AltoidaTappingTestXmlProcessor(),
                            AltoidaMetadataXmlProcessor()
//                            ,
//                            AltoidaTestMetricsProcessor(AltoidaTestMetricsProcessor.AltoidaTestCategory.BIT, "connect_upload_altoida_bit_metrics"),
//                            AltoidaTestMetricsProcessor(AltoidaTestMetricsProcessor.AltoidaTestCategory.DOT, "connect_upload_altoida_dot_metrics"),
                    ),
            ),

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
            ZipFileProcessorFactory(
                    sourceType,
                    zipEntryProcessors = listOf(
                            CsvFileProcessorFactory(
                                    csvProcessorFactories = listOf(
                                            AltoidaAccelerationCsvProcessor(),
                                            AltoidaAttitudeCsvProcessor(),
                                            AltoidaBlinkCsvProcessor(),
                                            AltoidaEyeTrackingCsvProcessor(),
                                            AltoidaGravityCsvProcessor(),
                                            AltoidaMagneticFieldCsvProcessor(),
                                            AltoidaPathCsvProcessor(),
                                            AltoidaMotorBubbleCsvProcessor(),
                                            AltoidaTouchScreenCsvProcessor(),
                                    )
                            )
                    ),
                    allowUnmappedFiles = true,
            ),
    )
}

