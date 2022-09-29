package org.radarbase.connect.upload.converter.axivity

import org.radarbase.connect.upload.api.SourceTypeDTO
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarbase.connect.upload.converter.archive.ArchiveProcessorFactory.Companion.sevenZipFactory
import org.radarbase.connect.upload.converter.archive.ArchiveProcessorFactory.Companion.zipFactory
import org.radarbase.connect.upload.logging.LogRepository

class AxivityConverterFactory : ConverterFactory {
    override val sourceType: String = "axivity"

    override fun fileProcessorFactories(
        settings: Map<String, String>,
        connectorConfig: SourceTypeDTO,
        logRepository: LogRepository,
    ): List<FileProcessorFactory> {
        val processors = createProcessors(connectorConfig.configuration ?: emptyMap())
        val cwaProcessors = listOf(CwaFileProcessorFactory(logRepository, processors))

        return listOf(
            zipFactory(sourceType, entryProcessors = cwaProcessors),
            sevenZipFactory(sourceType, entryProcessors = cwaProcessors),
        )
    }

    private fun createProcessors(
        config: Map<String, String>
    ): List<CwaFileProcessorFactory.CwaBlockProcessor> = processors
        .filter { (property, _) -> config[property]?.toBoolean() != false }
        .map { (_, generator) -> generator() }

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
