package org.radarbase.connect.upload.converter.axivity

import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarbase.connect.upload.converter.LogRepository
import org.radarbase.connect.upload.converter.RecordLogger
import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.axivity.newcastle.CwaBlock
import org.radarbase.connect.upload.converter.axivity.newcastle.CwaReader
import org.slf4j.LoggerFactory
import java.io.InputStream

class CwaFileProcessorFactory(
        val logRepository: LogRepository,
        private val dataBlockProcessors: Set<CwaBlockProcessor>
) : FileProcessorFactory {
    private val metadataProcessor = MetadataCwaReaderProcessor()

    override fun matches(contents: ContentsDTO): Boolean = contents.fileName.endsWith(".cwa", ignoreCase = true)

    override fun createProcessor(record: RecordDTO) = CwaProcessor(record)

    inner class CwaProcessor(record: RecordDTO) : FileProcessorFactory.FileProcessor {
        val recordLogger = logRepository.createLogger(logger, record.id!!)

        override fun processData(contents: ContentsDTO, inputStream: InputStream, timeReceived: Double): List<TopicData> {
            val cwaReader = CwaReader(inputStream)

            val data = generateSequence { cwaReader.peekBlock() }
                    .flatMap { block -> processBlock(block, timeReceived) }
                    .toList()

            val firstTime = data.firstOrNull()?.value?.get(0) as? Double
                    ?: timeReceived

            val metadata = metadataProcessor.processReader(cwaReader, firstTime, timeReceived)

            return metadata + data
        }

        private fun processBlock(block: CwaBlock, timeReceived: Double): Sequence<TopicData> {
            val result = if (block.isDataBlock) {
                dataBlockProcessors.flatMap { processor ->
                    processor.processBlock(recordLogger, block, timeReceived)
                }.asSequence()
            } else emptySequence()

            block.invalidate()
            return result
        }
    }

    interface CwaBlockProcessor {
        fun processBlock(recordLogger: RecordLogger, block: CwaBlock, timeReceived: Double): List<TopicData>

        fun CwaBlock.startTime(): Double = timestampValues[0] / 1000.0
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CwaProcessor::class.java)
    }
}
