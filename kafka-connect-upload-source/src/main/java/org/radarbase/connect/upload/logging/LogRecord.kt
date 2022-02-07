package org.radarbase.connect.upload.logging

import java.time.Instant

data class LogRecord(
    val logLevel: LogLevel,
    val message: String,
    val time: Instant = Instant.now(),
)
