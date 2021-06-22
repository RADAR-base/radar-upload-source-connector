package org.radarbase.connect.upload.logging

import okio.BufferedSink

data class Log(
    val recordId: Long,
    val records: Collection<LogRecord>,
) {
    fun asString(writer: BufferedSink) {
        records.forEach { log ->
            writer.writeUtf8("${log.time} - [${log.logLevel}] ${log.message}\n")
        }
    }
}
