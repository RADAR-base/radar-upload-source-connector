package org.radarbase.connect.upload.converter

import org.apache.avro.generic.IndexedRecord
import org.apache.kafka.connect.source.SourceRecord
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.api.SourceTypeDTO
import org.radarbase.connect.upload.api.UploadBackendClient
import java.io.Closeable

/**
 * Converter for each source-type
 */
interface Converter : Closeable {
    val sourceType: String

    fun initialize(connectorConfig: SourceTypeDTO, client: UploadBackendClient, settings: Map<String, String>)

    // convert and add logs return result
    fun convert(record: RecordDTO): ConversionResult

    fun getPartition(): MutableMap<String, Any>

    companion object {
        val END_OF_RECORD_KEY = "endOfRecord"
        val RECORD_ID_KEY = "recordId"
        val REVISION_KEY = "versionId"
    }
}

data class ConversionResult(val record: RecordDTO, val result: List<SourceRecord>?)


data class TopicData(
        var endOfFileOffSet: Boolean,
        val topic: String,
        val value: IndexedRecord)
