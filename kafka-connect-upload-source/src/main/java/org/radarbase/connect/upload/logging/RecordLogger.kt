package org.radarbase.connect.upload.logging

interface RecordLogger {
    /** Print an info log message. */
    fun info(logMessage: String)
    /** Print an debug log message. */
    fun debug(logMessage: String)
    /** Print an warn log message. */
    fun warn(logMessage: String)
    /** Print an error log message. The stack trace of the exception is included. */
    fun error(logMessage: String, exe: Throwable? = null)
}
