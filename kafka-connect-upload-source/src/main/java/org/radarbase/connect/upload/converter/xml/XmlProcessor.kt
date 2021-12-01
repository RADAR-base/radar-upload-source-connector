package org.radarbase.connect.upload.converter.xml

import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.ConverterFactory
import org.radarbase.connect.upload.converter.FileProcessor
import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.xml.StatelessXmlLineProcessor
import org.radarbase.connect.upload.exception.InvalidFormatException
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.IOException
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory


/**
 * Base class to process XML files with multiple possible XML processors.
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
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()

        val contentProcessorsFactories = processorFactories
                .filter { it.matches(context.contents) }

       if (context.contents.size == 0L) {
           throw IOException("Cannot read empty XML file ${context.fileName}")
       }

        val doc: Element = builder.parse(inputStream).documentElement

        processXml(doc, contentProcessorsFactories, context).forEach(produce)

    }

    private fun processXml(
            root: Element,
            contentProcessorsFactories: List<StatelessXmlLineProcessor>,
            context: ConverterFactory.ContentsContext,
    ): Sequence<TopicData> {

        val header: List<String> = emptyList()

        val processors = contentProcessorsFactories.createLineProcessors(context, header)

        return processors
                .asSequence()
                .flatMap { it.convertToRecord(root, context.timeReceived) }
    }

    private fun List<StatelessXmlLineProcessor>.createLineProcessors(
            context: ConverterFactory.ContentsContext,
            header: List<String>,
    ): List<StatelessXmlLineProcessor.Processor> {
        val processors = this
                .map { it.createLineProcessor(context) }

        if (processors.isEmpty()) {
            throw InvalidFormatException("For file ${context.fileName} in record ${context.id}, cannot find XML processor that matches header $header")
        }

        return processors
    }
}
