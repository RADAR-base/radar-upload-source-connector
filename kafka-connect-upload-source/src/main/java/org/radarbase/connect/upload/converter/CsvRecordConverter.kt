package org.radarbase.connect.upload.converter

import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import okhttp3.ResponseBody
import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.slf4j.LoggerFactory
import java.io.IOException

abstract class CsvRecordConverter(sourceType: String): RecordConverter(sourceType) {

    override fun processData(contents: ContentsDTO, responseBody: ResponseBody, record: RecordDTO, timeReceived: Double, topic: String): List<TopicData> {
        logger.debug("Retrieved file content from record id ${record.id} and filename ${contents.fileName}")
        val inputStream = responseBody.byteStream()
        val reader = CSVReaderBuilder(inputStream.bufferedReader())
                .withCSVParser(CSVParserBuilder().withSeparator(',').build())
                .build()
        val convertedTopicData = mutableListOf<TopicData>()
        try {
            val header = reader.readNext()
            // do we want to validate headers?
            validateHeaderSchema(header.asList())

            var line = reader.readNext()
            while (line != null) {
                convertedTopicData.add(convertLineToRecord(header.zip(line).toMap(), timeReceived, topic))
                line = reader.readNext()
            }
            convertedTopicData.last().endOfFileOffSet = true
        } catch (exe: IOException) {
            logger.warn("Something went wrong while processing contents of file ${contents.fileName}", exe)
        } finally {
            logger.debug("Closing resources of content ${contents.fileName}")
            inputStream.close()
            responseBody.close()
        }
        return convertedTopicData
    }

    abstract fun validateHeaderSchema(csvHeader: List<String>): Boolean

    abstract fun convertLineToRecord(lineValues: Map<String, String>, timeReceived: Double, topic: String): TopicData

    companion object {
        private val logger = LoggerFactory.getLogger(CsvRecordConverter::class.java)
    }
}
