package org.radarbase.connect.upload.converter

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Parser for CSV time fields */
interface TimeFieldParser {
    val fieldName: String

    fun timeFromString(timestamp: String): Double
    fun time(line: Map<String, String>): Double =
            timeFromString(line.getValue(fieldName))

    /** CSV timestamp parser, assuming that the timestamp is provided as milliseconds since the Unix Epoch. */
    class EpochMillisParser(override val fieldName: String = "TIMESTAMP") : TimeFieldParser {
        override fun timeFromString(timestamp: String): Double =
            timestamp.toDouble() / 1000.0
    }

    /** CSV timestamp parser using given date-time format. */
    class DateFormatParser(private val timeFormatter: DateTimeFormatter, override val fieldName: String = "TIMESTAMP") : TimeFieldParser {
        override fun timeFromString(timestamp: String): Double =
                Instant.from(timeFormatter.parse(timestamp))
                        .toEpochMilli() / 1000.0

        companion object {
            fun String.formatTimeFieldParser(fieldName: String = "TIMESTAMP"): TimeFieldParser = DateFormatParser(
                    DateTimeFormatter.ofPattern(this).withZone(ZoneOffset.UTC),
                    fieldName)
        }
    }
}
