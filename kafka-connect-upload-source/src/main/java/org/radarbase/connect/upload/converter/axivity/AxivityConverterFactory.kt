package org.radarbase.connect.upload.converter.axivity

import org.radarbase.connect.upload.api.SourceTypeDTO
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarbase.connect.upload.converter.LogRepository
import org.radarbase.connect.upload.converter.ZipFileProcessorFactory
import org.radarbase.connect.upload.converter.axivity.newcastle.CwaCsvInputStream
import org.slf4j.LoggerFactory

class AxivityConverterFactory : ConverterFactory {
    override val sourceType: String = "axivity"

    override fun fileProcessorFactories(
            settings: Map<String, String>,
            connectorConfig: SourceTypeDTO,
            logRepository: LogRepository
    ): List<FileProcessorFactory> {
        checkNotNull(connectorConfig.configuration)
        val (readOption, header) = cwaReadOptions(connectorConfig)
        return listOf(
                ZipFileProcessorFactory(
                        listOf(
                                CwaFileProcessorFactory(
                                        listOf(AxivityCsvProcessor(header)),
                                        logRepository,
                                        header,
                                        readOption)),
                        logRepository))
    }

    private fun cwaReadOptions(connectorConfig: SourceTypeDTO): Pair<Int, List<String>> {
        // default to option 0 (Acceleration data)
        val readOption = 0
        val cwaHeader = mutableListOf("TIMESTAMP", "X", "Y", "Z")

        // optionally read additional data based on the config
        // this will compute the corresponding value for option and headers for the csv line that will be created.
        val readAlso = connectorConfig.configuration?.getOrDefault("readAlso", null)
        logger.info("Reading $readAlso from a cwa file.")
        readAlso?.split(" ")?.forEach {
            when (it) {
                "--temp" -> {
                    readOption.or(CwaCsvInputStream.OPTIONS_TEMP)
                    cwaHeader.add("TEMPERATURE")
                }
                "--light" -> {
                    readOption.or(CwaCsvInputStream.OPTIONS_LIGHT)
                    cwaHeader.add("LIGHT")
                }
                "--battery" -> {
                    readOption.or(CwaCsvInputStream.OPTIONS_BATT)
                    cwaHeader.add("BATTERY")
                }
                "--events" -> {
                    readOption.or(CwaCsvInputStream.OPTIONS_EVENTS)
                    cwaHeader.add("EVENTS")
                }
            }
        }
        logger.info("Setting CWA read option to $readOption and headers to $cwaHeader")
        return Pair(readOption, cwaHeader)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CwaFileProcessorFactory.CwaProcessor::class.java)
    }
}

