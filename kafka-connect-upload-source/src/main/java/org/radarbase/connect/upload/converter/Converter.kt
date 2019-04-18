package org.radarbase.connect.upload

import org.apache.kafka.connect.source.SourceRecord
import org.radarbase.upload.api.RecordDTO
import java.io.Closeable

interface Converter: Closeable {
    val name: String

    fun initialize(settings: Map<String, String>, client: UploadBackendClient) {}

    fun convert(record: RecordDTO): ConversionResult
}

data class ConversionResult(val record: RecordDTO, val result: List<SourceRecord>?)
