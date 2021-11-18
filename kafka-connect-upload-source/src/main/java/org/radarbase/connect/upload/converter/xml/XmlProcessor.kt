package org.radarbase.connect.upload.converter.csv

import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.FileProcessor
import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.xml.StatelessXmlLineProcessor
import org.radarbase.connect.upload.exception.InvalidFormatException
import org.w3c.dom.Document
import java.io.IOException
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory


/**
 * Base class to process CSV files with multiple possible per-CSV-line processors.
 */
open class XmlProcessor(
        private val record: RecordDTO,
        private val processorFactories: List<StatelessXmlLineProcessor>,
): FileProcessor {

    override fun processData(
            context: ConverterFactory.ContentsContext,
            inputStream: InputStream,
            produce: (TopicData) -> Unit,
    ) {
        // create a new DocumentBuilderFactory
        val factory = DocumentBuilderFactory.newInstance()

        // use the factory to create a documentbuilder
        val builder = factory.newDocumentBuilder()

        val contentProcessorsFactories = processorFactories
                .filter { it.matches(context.contents) }

       if (context.contents.size == 0L) {
           return
       }

        val doc: Document = builder.parse(inputStream)

        processXml(doc, contentProcessorsFactories, context).forEach(produce)

    }

    /**
     * Check whether having an empty CSV file is allowed.
     * @throws IOException if the CSV file should have contents.
     */
    private fun checkCsvEmpty(
            contentProcessorsFactories: List<CsvLineProcessorFactory>,
            context: ConverterFactory.ContentsContext,
    ) {
        if (contentProcessorsFactories.all { it.optional }) {
            context.logger.debug("Skipping optional file ${context.fileName}")
        } else {
            throw IOException("Cannot read empty CSV file ${context.fileName}")
        }
    }

    private fun processXml(
            document: Document,
            contentProcessorsFactories: List<StatelessXmlLineProcessor>,
            context: ConverterFactory.ContentsContext,
    ): Sequence<TopicData> {

        val header: List<String> = emptyList()

        val processors = contentProcessorsFactories.createLineProcessors(context, header)

        return processors
                .asSequence()
                .flatMap { it.convertToRecord(document, context.timeReceived) }
    }

    protected open fun CsvLineProcessorFactory.checkHeader(
            contents: ContentsDTO,
            header: List<String>,
    ): Boolean = when {
        matches(header) -> true
        headerMustMatch -> throw InvalidFormatException(
                """
            CSV header of file ${contents.fileName} in record ${record.id} did not match processor $javaClass
                Found:    $header
                Expected: ${this.header}
            """.trimIndent()
        )
        else -> false
    }

    private fun List<StatelessXmlLineProcessor>.createLineProcessors(
            context: ConverterFactory.ContentsContext,
            header: List<String>,
    ): List<StatelessXmlLineProcessor.Processor> {
        val processors = this
                .map { it.createLineProcessor(context) }

        if (processors.isEmpty()) {
            throw InvalidFormatException("For file ${context.fileName} in record ${context.id}, cannot find CSV processor that matches header $header")
        }

        return processors
    }
}
