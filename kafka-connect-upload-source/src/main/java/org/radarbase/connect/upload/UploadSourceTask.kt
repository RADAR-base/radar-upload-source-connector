package org.radarbase.connect.upload

import org.apache.kafka.connect.errors.ConnectException
import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.source.SourceTask
import org.radarbase.connect.upload.api.PollDTO
import org.radarbase.connect.upload.api.RecordMetadataDTO
import org.radarbase.connect.upload.api.UploadBackendClient
import org.radarbase.connect.upload.converter.Converter
import org.radarbase.connect.upload.converter.Converter.Companion.END_OF_RECORD_KEY
import org.radarbase.connect.upload.converter.Converter.Companion.RECORD_ID_KEY
import org.radarbase.connect.upload.converter.Converter.Companion.REVISION_KEY
import org.radarbase.connect.upload.util.VersionUtil
import org.slf4j.LoggerFactory

class UploadSourceTask : SourceTask() {
    private lateinit var uploadClient: UploadBackendClient
    private lateinit var converters: List<Converter>

    override fun start(props: Map<String, String>?) {

        val connectConfig = UploadSourceConnectorConfig(props!!)
        uploadClient = UploadBackendClient(
                connectConfig.getAuthorizer(),
                connectConfig.getHttpClient(),
                connectConfig.getBaseUrl())

        // init converters if configured
        val converters = connectConfig.getConverterClasses().map {
            try {
                val converterClass = Class.forName(it)
                converterClass.getDeclaredConstructor().newInstance() as Converter
            } catch (exe: Exception) {
                when (exe) {
                    is ClassNotFoundException -> throw ConnectException("Converter class ${it} not found in class path", exe)
                    is IllegalAccessException -> throw ConnectException("Converter class ${it} could not be instantiated", exe)
                    is InstantiationException -> throw ConnectException("Converter class ${it} could not be instantiated", exe)
                    else -> throw ConnectException("Cannot successfully initialize converter ${it}", exe)
                }
            }
        }.toList()

        for (converter in converters) {
            val config = uploadClient.requestConnectorConfig(converter.sourceType)
            converter.initialize(config, uploadClient, props)
        }

    }

    override fun stop() {
        logger.debug("Stopping source task")
        uploadClient.close()
        converters.forEach(Converter::close)
    }

    override fun version(): String = VersionUtil.getVersion()

    override fun poll(): List<SourceRecord> {
        val records = uploadClient.pollRecords(PollDTO(1, converters.map { it.sourceType })).records

        val allResults = ArrayList<SourceRecord>(records.size)

        for (record in records) {
            val converter = converters.find { it.sourceType == record.sourceType }

            if (converter == null) {
                uploadClient.updateStatus(record.id!!, record.metadata!!.copy(status = "FAILED", message = "Source type ${record.sourceType} not found."))
                continue
            } else {
                record.metadata = uploadClient.updateStatus(record.id!!, record.metadata!!.copy(status = "PROCESSING"))
            }

            val result = converter.convert(record)
            result.result?.let {
                allResults.addAll(it)
            }
        }

        return allResults
    }

    override fun commitRecord(record: SourceRecord?) {
        record ?: return

        val offset = record.sourceOffset()

        val recordId = offset[RECORD_ID_KEY] as? Number ?: return
        val revision = offset[REVISION_KEY] as? Number ?: return
        val endOfRecord = offset[END_OF_RECORD_KEY] as? Boolean ?: return

        if (endOfRecord) {
            logger.info("Committing last record of Record ${recordId}, with Revision ${revision.toInt()}")
            uploadClient.updateStatus(recordId.toLong(), RecordMetadataDTO(revision = revision.toInt(), status = "SUCCESS"))
        }

    }

    companion object {
        private val logger = LoggerFactory.getLogger(UploadSourceTask::class.java)
    }

}
