/*
 *  Copyright 2019 The Hyve
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.radarbase.connect.upload.converter.phone

import org.radarbase.connect.upload.api.SourceTypeDTO
import org.radarbase.connect.upload.converter.*

class AccelerometerZipConverterFactory : ConverterFactory {
    override val sourceType: String = "acceleration-zip"

    override fun fileProcessorFactories(
        settings: Map<String, String>,
        connectorConfig: SourceTypeDTO,
        logRepository: LogRepository
    ): List<FileProcessorFactory> {
        val csvLineProcessors  = listOf<CsvLineProcessorFactory>(
                AccelerometerCsvProcessor())

        val csvProcessors = listOf(CsvFileProcessorFactory(csvLineProcessors, logRepository))

        return listOf(
            ZipFileProcessorFactory(sourceType, csvProcessors, logRepository),
        )
    }
}
