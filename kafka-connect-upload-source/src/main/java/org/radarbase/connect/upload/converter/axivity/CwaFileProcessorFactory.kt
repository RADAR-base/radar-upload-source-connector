package org.radarbase.connect.upload.converter.axivity

import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.FileProcessor
import org.radarbase.connect.upload.converter.FileProcessorFactory
import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.axivity.newcastle.CwaBlock
import org.radarbase.connect.upload.converter.axivity.newcastle.CwaReader
import org.radarbase.connect.upload.logging.LogRepository
import org.radarbase.connect.upload.logging.RecordLogger
import org.slf4j.LoggerFactory
import java.io.InputStream

class CwaFileProcessorFactory(
    val logRepository: LogRepository,
    private val dataBlockProcessors: List<CwaBlockProcessor>,
) : FileProcessorFactory {
    private val metadataProcessor = MetadataCwaReaderProcessor()

    override fun matches(contents: ContentsDTO): Boolean = contents.fileName.endsWith(".cwa", ignoreCase = true)

    override fun createProcessor(record: RecordDTO) = CwaProcessor(record)

    inner class CwaProcessor(record: RecordDTO) : FileProcessor {
        val recordLogger = logRepository.createLogger(logger, record.id!!)

        override fun processData(
            context: ConverterFactory.ContentsContext,
            inputStream: InputStream,
            produce: (TopicData) -> Unit,
        ) {
            val cwaReader = CwaReader(inputStream)

            try {
                var firstTime = context.timeReceived

                generateSequence { cwaReader.peekBlock() }
                    .flatMap { block -> processBlock(block, context.timeReceived) }
                    .forEachIndexed { idx, data ->
                        if (idx == 0) {
                            val timeValue = data.value.get(0) as? Double
                            if (timeValue != null) {
                                firstTime = timeValue
                            }
                        }
                        produce(data)
                    }

                metadataProcessor.processReader(cwaReader, firstTime, context.timeReceived)
                    .forEach(produce)
            } finally {
                cwaReader.close()
            }
        }

        private fun processBlock(
            block: CwaBlock,
            timeReceived: Double,
        ): Sequence<TopicData> {
            val result = if (block.isDataBlock) {
                dataBlockProcessors
                    .asSequence()
                    .flatMap { processor ->
                        processor.processBlock(recordLogger, block, timeReceived)
                    }
            } else {
                emptySequence()
            }

            block.invalidate()
            return result
        }
    }

    interface CwaBlockProcessor {
        fun processBlock(
            recordLogger: RecordLogger,
            block: CwaBlock,
            timeReceived: Double,
        ): Sequence<TopicData>

        val CwaBlock.startTime: Double
            get() = timestampValues[0] / 1000.0
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CwaProcessor::class.java)
    }
}
