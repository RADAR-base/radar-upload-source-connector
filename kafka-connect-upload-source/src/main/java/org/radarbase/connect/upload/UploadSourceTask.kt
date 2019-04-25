package org.radarbase.connect.upload

import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.source.SourceTask
import org.radarbase.connect.upload.converter.Converter
import org.radarbase.connect.upload.api.PollDTO
import org.radarbase.connect.upload.api.RecordMetadataDTO
import org.radarbase.connect.upload.api.UploadBackendClient
import org.radarbase.connect.upload.util.VersionUtil

class UploadSourceTask: SourceTask() {
    private lateinit var uploadClient: UploadBackendClient
    private lateinit var converters: List<Converter>

    override fun start(props: Map<String, String>?) {

        val connectConfig = UploadSourceConnectorConfig(props!!)
        uploadClient = UploadBackendClient(connectConfig.getAuthorizer(), connectConfig.getHttpClient())
        // TODO init uploadClient
        // TODO init converters
    }

    override fun stop() {
        uploadClient.close()
        converters.forEach(Converter::close)
    }

    override fun version(): String = VersionUtil.getVersion()

    override fun poll(): List<SourceRecord> {
        val records = uploadClient.pollRecords(PollDTO(1, converters.map { it.name })).records

        val allResults = ArrayList<SourceRecord>(records.size)

        for (record in records) {
            val converter = converters.find { it.name == record.sourceType }

            if (converter == null) {
                uploadClient.updateStatus(record.id!!, record.metadata!!.copy(status = "FAILED", message = "Source type ${record.sourceType} not found."))
                continue
            } else {
                record.metadata = uploadClient.updateStatus(record.id!!, record.metadata!!.copy(status = "PROCESSING"))
            }

            val result = converter.convert(record)
            uploadClient.addLogs(record.id!!, record.metadata!!)
            result.result?.let {
                allResults.addAll(it)
            }
        }

        return allResults
    }

    override fun commitRecord(record: SourceRecord?) {
        record ?: return

        val offset = record.sourceOffset()

        val recordId = offset["recordId"] as? Number ?: return
        val revision = offset["revision"] as? Number ?: return

        uploadClient.updateStatus(recordId.toLong(), RecordMetadataDTO(revision = revision.toInt(), status = "SUCCESS"))
    }
}