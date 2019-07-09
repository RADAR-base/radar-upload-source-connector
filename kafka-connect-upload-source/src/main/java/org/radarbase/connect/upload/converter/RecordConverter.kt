package org.radarbase.connect.upload.converter

import io.confluent.connect.avro.AvroData
import okhttp3.ResponseBody
import org.apache.kafka.connect.data.SchemaAndValue
import org.apache.kafka.connect.source.SourceRecord
import org.radarbase.connect.upload.UploadSourceConnectorConfig
import org.radarbase.connect.upload.api.*
import org.radarbase.connect.upload.converter.Converter.Companion.END_OF_RECORD_KEY
import org.radarbase.connect.upload.converter.Converter.Companion.RECORD_ID_KEY
import org.radarbase.connect.upload.converter.Converter.Companion.REVISION_KEY
import org.radarbase.connect.upload.api.LogLevel.*
import org.radarcns.kafka.ObservationKey
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.Instant


abstract class RecordConverter(override val sourceType: String, val avroData: AvroData = AvroData(20)) : Converter {

    private lateinit var connectorConfig: SourceTypeDTO
    private lateinit var client: UploadBackendClient
    private lateinit var settings: Map<String, String>
    private lateinit var topic: String
    private lateinit var logsRepository: MutableList<Log>
    override fun initialize(connectorConfig: SourceTypeDTO, client: UploadBackendClient, settings: Map<String, String>) {
        this.connectorConfig = connectorConfig
        this.client = client
        this.settings = settings

        if (this.connectorConfig.topics?.size == 1) {
            this.topic = this.connectorConfig.topics?.first()!!
        }

        logsRepository = mutableListOf()
    }

    override fun close() {
        this.client.close()
    }

    fun log(logLevel: LogLevel, logMessage: String) {
        logsRepository.add(Log(logLevel, logMessage))

        when (logLevel) {
            INFO -> logger.info(logMessage)
            WARN -> logger.warn(logMessage)
            DEBUG -> logger.debug(logMessage)
            ERROR -> logger.error(logMessage)
        }
    }

    override fun convert(record: RecordDTO): ConversionResult {
        log(INFO, "Converting record : record-id ${record.id}")

        record.validateRecord()

        val key = record.computeObservationKey(avroData)

        val contents = record.data!!.contents!!

        val sourceRecords = contents.asSequence().map contentMap@{

            val fileStream = client.retrieveFile(record, it.fileName) // make sure zip files are unpacked first!!
                    ?: throw IOException("Cannot retrieve file ${it.fileName} from record with id ${record.id}")
            val timeReceived = Instant.now().epochSecond

            return@contentMap processData(it, fileStream, record, timeReceived.toDouble())
                    .map topicDataMap@{
                        val valRecord = avroData.toConnectData(it.value.schema, it.value)
                        val offset = mutableMapOf(
                                END_OF_RECORD_KEY to it.endOfFileOffSet,
                                RECORD_ID_KEY to record.id,
                                REVISION_KEY to record.metadata?.revision
                        )
                        // find the last record and set END_OF_RECORD_KEY to true, otherwise false
                        return@topicDataMap SourceRecord(getPartition(), offset, it.topic, key.schema(), key.value(), valRecord.schema(), valRecord.value())
                    }.toList()
        }.toList()
        commitLogs(record, client)
        return ConversionResult(record, sourceRecords.flatMap { it.toList() })
    }

    private fun commitLogs(record: RecordDTO, client: UploadBackendClient) {
        val logs = LogsDto().apply {
                contents = UploadSourceConnectorConfig.mapper.writeValueAsString(logsRepository)
        }
        logger.info(UploadSourceConnectorConfig.mapper.writeValueAsString(logsRepository))
        client.addLogs(record.id!!, logs)
    }

    /** process file content with the record data. The implementing method should close response-body. */
    abstract fun processData(contents: ContentsDTO, responseBody: ResponseBody, record: RecordDTO, timeReceived: Double)
            : List<TopicData>

    override fun getPartition(): MutableMap<String, Any> = mutableMapOf("source-type" to sourceType)

    private fun RecordDTO.validateRecord() {
        this.id ?: throw IllegalStateException("Record id cannot be null")
        this.metadata ?: throw IllegalStateException("Record meta-data cannot be null")
        this.data ?: throw IllegalStateException("Record data cannot be null")
        this.data?.contents ?: throw IllegalStateException("Record data has empty content")
    }

    private fun RecordDTO.computeObservationKey(avroData: AvroData): SchemaAndValue {
        val data = this.data
        data ?: throw IllegalStateException("Cannot process record without data")
        return avroData.toConnectData(
                ObservationKey.getClassSchema(),
                ObservationKey(
                        data.projectId,
                        data.userId,
                        data.sourceId
                )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RecordConverter::class.java)
    }


}
