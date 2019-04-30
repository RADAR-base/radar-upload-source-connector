package org.radarbase.connect.upload.converter

import io.confluent.connect.avro.AvroData
import org.apache.kafka.connect.source.SourceRecord
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.api.SourceTypeDTO
import org.radarbase.connect.upload.api.UploadBackendClient
import org.radarbase.connect.upload.converter.Converter.Companion.TIMESTAMP_OFFSET_KEY
import org.radarbase.connect.upload.record.RecordProcessor
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.lang.IllegalStateException
import java.time.Instant

abstract class RecordConverter(override val sourceType: String, val avroData: AvroData): Converter {

    private lateinit var connectorConfig: SourceTypeDTO
    private lateinit var client: UploadBackendClient
    private lateinit var settings: Map<String, String>

    override fun initialize(connectorConfig: SourceTypeDTO, client: UploadBackendClient, settings: Map<String, String>) {
        this.connectorConfig = connectorConfig
        this.client = client
        this.settings = settings
    }

    override fun close() {
        this.client.close()
    }

    override fun convert(record: RecordDTO): ConversionResult {
        val recordProcessor = RecordProcessor(record, client)

        recordProcessor.validateRecord()

        val key = recordProcessor.getObservationKey(avroData)

        val contents = record.data?.contents ?: throw IllegalStateException("No content found for record with id ${record.id}")

        val sourceRecords = contents.asSequence().map contentMap@ {

            val fileStream = client.retrieveFile(record, it.fileName)
                    ?: throw IOException("Cannot retrieve file ${it.fileName} from record with id ${record.id}")
            val timeReceived = Instant.now().epochSecond
            logger.debug("Retrieved file content from record id ${record.id} and filename ${it.fileName}")
            return@contentMap processData(fileStream, record, timeReceived.toDouble())
                    .map topicDataMap@ {
                        val valRecord = avroData.toConnectData(it.value.schema, it.value)
                        val offset = mapOf(TIMESTAMP_OFFSET_KEY to it.sourceOffSet.toEpochMilli())
                        return@topicDataMap SourceRecord(getPartition(), offset, it.topic, key.schema(), key.value(), valRecord.schema(), valRecord.value())
                    }.toList()
        }.toList()

        return ConversionResult(record.copy(
                metadata = record.metadata?.copy(
                        status = "SUCCEEDED"
                )
        ), sourceRecords.flatMap { it.toList() })
    }

    /** process file content with the record data. */
    abstract fun processData(inputStream: InputStream, record: RecordDTO, timeReceived: Double)
            : Sequence<TopicData>

    companion object {
        private val logger = LoggerFactory.getLogger(RecordConverter::class.java)
    }


}
