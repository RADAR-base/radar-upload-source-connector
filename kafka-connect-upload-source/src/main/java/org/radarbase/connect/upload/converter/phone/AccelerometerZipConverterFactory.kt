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
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarbase.connect.upload.converter.archive.ArchiveProcessorFactory.Companion.zipFactory
import org.radarbase.connect.upload.converter.csv.CsvFileProcessorFactory
import org.radarbase.connect.upload.logging.LogRepository

class AccelerometerZipConverterFactory : ConverterFactory {
    override val sourceType: String = "acceleration-zip"

    override fun fileProcessorFactories(
        settings: Map<String, String>,
        connectorConfig: SourceTypeDTO,
        logRepository: LogRepository,
    ): List<FileProcessorFactory> = listOf(
        zipFactory(
            sourceType,
            entryProcessors = listOf(
                CsvFileProcessorFactory(
                    csvProcessorFactories = listOf(
                        AccelerometerCsvProcessor(),
                    ),
                ),
            ),
        ),
    )
}
