package org.radarbase.connect.upload.converter.axivity

import org.radarbase.connect.upload.api.SourceTypeDTO
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarbase.connect.upload.converter.LogRepository
import org.radarbase.connect.upload.converter.ZipFileProcessorFactory

class AxivityConverterFactory : ConverterFactory {
    override val sourceType: String = "axivity"

    override fun fileProcessorFactories(
            settings: Map<String, String>,
            connectorConfig: SourceTypeDTO,
            logRepository: LogRepository
    ): List<FileProcessorFactory> {
        val fileProcessors = listOf(
                CwaFileProcessorFactory(listOf(AxivityCsvLineProcessor()), logRepository))
        return listOf(
                ZipFileProcessorFactory(fileProcessors, logRepository))
    }
}

