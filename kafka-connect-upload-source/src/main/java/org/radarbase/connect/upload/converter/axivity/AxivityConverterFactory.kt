package org.radarbase.connect.upload.converter.axivity

import org.radarbase.connect.upload.api.SourceTypeDTO
import org.radarbase.connect.upload.converter.*
import org.radarbase.connect.upload.converter.altoida.AltoidaAccelerationCsvProcessor
import org.radarbase.connect.upload.converter.altoida.AltoidaMetadataFileProcessor
import org.radarbase.connect.upload.converter.altoida.summary.AltoidaExportCsvProcessor

class AxivityConverterFactory : ConverterFactory {
    override val sourceType: String = "axivity"

    override fun fileProcessorFactories(
            settings: Map<String, String>,
            connectorConfig: SourceTypeDTO,
            logRepository: LogRepository
    ): List<FileProcessorFactory> {
        val fileProcessors = listOf(
                CwaFileProcessorFactory(listOf(AltoidaExportCsvProcessor()), logRepository))
        return listOf(
                ZipFileProcessorFactory(fileProcessors, logRepository)
                )
    }
}

