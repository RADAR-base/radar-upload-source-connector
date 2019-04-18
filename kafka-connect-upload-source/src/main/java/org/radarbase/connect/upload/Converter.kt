package org.radarbase.connect.upload

import org.apache.kafka.connect.source.SourceRecord
import org.apache.kafka.connect.source.SourceTask
import java.io.Closeable
import java.io.InputStream
import java.util.stream.Stream

interface Converter: Closeable {
    val name: String

    fun initialize(settings: Map<String, String>) {}

    fun convert(input: InputStream, record: Any): List<SourceRecord>
}
