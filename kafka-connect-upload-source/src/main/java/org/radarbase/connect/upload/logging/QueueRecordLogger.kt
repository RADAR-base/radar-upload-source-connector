package org.radarbase.connect.upload.logging

import org.slf4j.Logger
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.*

class QueueRecordLogger(
    private val logger: Logger,
    private val recordId: Long,
    private val container: Queue<LogRecord>,
) : RecordLogger {
    override fun info(logMessage: String) {
        container.add(LogRecord(LogLevel.INFO, logMessage))
        logger.info("[record {}] {}", recordId, logMessage)
    }

    override fun debug(logMessage: String) {
        container.add(LogRecord(LogLevel.DEBUG, logMessage))
        logger.debug("[record {}] {}", recordId, logMessage)
    }

    override fun warn(logMessage: String) {
        container.add(LogRecord(LogLevel.WARN, logMessage))
        logger.warn("[record {}] {}", recordId, logMessage)
    }

    override fun error(logMessage: String, exe: Throwable?) {
        val message = if (exe != null) {
            val trace = ByteArrayOutputStream().use { byteOut ->
                PrintStream(byteOut).use { printOut ->
                    exe.printStackTrace(printOut)
                }
                byteOut.toString("UTF-8")
            }
            "$logMessage: $exe$trace"
        } else {
            logMessage
        }

        container.add(LogRecord(LogLevel.ERROR, message))
        logger.error("[record {}] {}", recordId, logMessage, exe)
    }
}
