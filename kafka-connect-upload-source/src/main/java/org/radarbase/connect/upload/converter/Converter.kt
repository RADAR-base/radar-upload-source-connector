package org.radarbase.connect.upload.converter

import org.apache.kafka.connect.source.SourceRecord
import org.radarbase.connect.upload.api.UploadBackendClient
import org.radarbase.connect.upload.api.RecordDTO
import java.io.Closeable

/**
 * Converter for each source-type
 */
interface Converter: Closeable {
    val sourceType: String

    fun initialize(settings: Map<String, String>, client: UploadBackendClient) {}

    // convert and add logs return result
    fun convert(record: RecordDTO): ConversionResult
}

data class ConversionResult(val record: RecordDTO, val result: List<SourceRecord>?)
