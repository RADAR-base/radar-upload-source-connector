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
import org.radarbase.connect.upload.converter.altoida.summary.AltoidaDomainResultProcessor
import org.radarbase.connect.upload.converter.altoida.summary.AltoidaHeaderProcessor
import org.radarbase.connect.upload.converter.altoida.summary.AltoidaSummaryProcessor
import org.radarbase.connect.upload.converter.altoida.summary.AltoidaTestMetricsProcessor

class AltoidaConverterFactory : ConverterFactory {
    override val sourceType: String = "altoida"

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
                AltoidaMagneticFieldCsvProcessor(),
                AltoidaObjectCsvProcessor(),
                AltoidaPathCsvProcessor(),
                AltoidaRotationCsvProcessor(),
                AltoidaTapScreenCsvProcessor(),
                AltoidaTouchScreenCsvProcessor(),
                AltoidaEyeTrackingCsvProcessor(),
                AltoidaBlinkCsvProcessor())

        val csvExportProcessors = listOf(
                AltoidaSummaryProcessor(),
                AltoidaDomainResultProcessor(),
                AltoidaTestMetricsProcessor(AltoidaTestMetricsProcessor.AltoidaTestCategory.BIT, "connect_upload_altoida_bit_metrics"),
                AltoidaTestMetricsProcessor(AltoidaTestMetricsProcessor.AltoidaTestCategory.DOT, "connect_upload_altoida_dot_metrics"))

        val csvExportHeaderProcessors = listOf(
                AltoidaHeaderProcessor()
        )

        val fileProcessors = listOf(
                CsvFileProcessorFactory(processorFactories = csvLineProcessors, logRepository = logRepository),
                AltoidaMetadataFileProcessor(logRepository))

        val filePreProcessors = listOf(
                AltoidaCsvFilePreProcessor(logRepository)
        )

        return listOf(
                ZipFileProcessorFactory(fileProcessors, logRepository),
                CsvFileProcessorFactory(csvExportHeaderProcessors, csvExportProcessors, logRepository),
                FilePreProcessorFactory(filePreProcessors, logRepository)
        )
    }
}

