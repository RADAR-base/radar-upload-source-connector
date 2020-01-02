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
        val processors = createProcessors(
                connectorConfig.configuration ?: emptyMap())

        return listOf(
                ZipFileProcessorFactory(
                        listOf(
                                CwaFileProcessorFactory(
                                        logRepository,
                                        processors.toSet())),
                        logRepository))
    }

    private fun createProcessors(config: Map<String, String>): Set<CwaFileProcessorFactory.CwaBlockProcessor> {
        val processors = mutableListOf<CwaFileProcessorFactory.CwaBlockProcessor>(
                AccelerationCwaBlockProcessor())

        // default to true
        if (config["readLight"]?.toBoolean() != false) processors += LightCwaBlockProcessor()
        if (config["readBattery"]?.toBoolean() != false) processors += BatteryLevelCwaBlockProcessor()
        if (config["readTemperature"]?.toBoolean() != false) processors += TemperatureCwaBlockProcessor()
        if (config["readEvents"]?.toBoolean() != false) processors += EventsCwaBlockProcessor()

        return processors.toSet()
    }
}
