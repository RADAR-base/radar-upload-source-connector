package org.radarbase.connect.upload.converter.axivity

import org.radarbase.connect.upload.api.SourceTypeDTO
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarbase.connect.upload.logging.LogRepository
import org.radarbase.connect.upload.converter.zip.ZipFileProcessorFactory

class AxivityConverterFactory : ConverterFactory {
    override val sourceType: String = "axivity"

    override fun fileProcessorFactories(
            settings: Map<String, String>,
            connectorConfig: SourceTypeDTO,
            logRepository: LogRepository
    ): List<FileProcessorFactory> {
        val processors = createProcessors(connectorConfig.configuration ?: emptyMap())

        return listOf(ZipFileProcessorFactory(
            sourceType,
            zipEntryProcessors = listOf(CwaFileProcessorFactory(logRepository, processors)),
        ))
    }

    private fun createProcessors(
        config: Map<String, String>
    ): List<CwaFileProcessorFactory.CwaBlockProcessor> {
        return processors
            .filter { (property, _) -> config[property]?.toBoolean() != false }
            .map { (_, generator) -> generator() }
    }

    companion object {
        private val processors = mapOf<String, () -> CwaFileProcessorFactory.CwaBlockProcessor> (
            "readAcceleration" to ::AccelerationCwaBlockProcessor,
            "readLight" to ::LightCwaBlockProcessor,
            "readBattery" to ::BatteryLevelCwaBlockProcessor,
            "readTemperature" to ::TemperatureCwaBlockProcessor,
            "readEvents" to ::EventsCwaBlockProcessor,
        )
    }
}
