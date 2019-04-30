package org.radarbase.connect.upload.converter

import org.apache.avro.generic.IndexedRecord
import org.apache.kafka.connect.source.SourceRecord
import org.radarbase.connect.upload.api.UploadBackendClient
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.api.SourceTypeDTO
import java.io.Closeable
import java.time.Instant

/**
 * Converter for each source-type
 */
interface Converter: Closeable {
    val sourceType: String

    fun initialize(connectorConfig: SourceTypeDTO, client: UploadBackendClient, settings: Map<String, String>)

    // convert and add logs return result
    fun convert(record: RecordDTO): ConversionResult

    fun getPartition(): MutableMap<String, Any>

    companion object {
        val TIMESTAMP_OFFSET_KEY = "timestamp"
    }
}

data class ConversionResult(val record: RecordDTO, val result: List<SourceRecord>?)


data class TopicData(
        val sourceOffSet: Instant,
        val topic: String,
        val value: IndexedRecord)
