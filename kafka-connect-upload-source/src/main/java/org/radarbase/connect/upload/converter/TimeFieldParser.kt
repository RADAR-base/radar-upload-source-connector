package org.radarbase.connect.upload.converter

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Parser for CSV time fields */
interface TimeFieldParser {
    fun time(line: Map<String, String>): Double
    fun timeFromString(timestamp: String): Double

    /** CSV timestamp parser, assuming that the timestamp is provided as milliseconds since the Unix Epoch. */
    class EpochMillisParser(private val fieldName: String = "TIMESTAMP") : TimeFieldParser {
        override fun time(line: Map<String, String>): Double =
                line.getValue(fieldName).toDouble() / 1000.0

        override fun timeFromString(timestamp: String): Double {
            TODO("Not yet implemented")
        }
    }

    /** CSV timestamp parser using given date-time format. */
    class DateFormatParser(private val timeFormatter: DateTimeFormatter, private val fieldName: String = "TIMESTAMP") : TimeFieldParser {
        override fun time(line: Map<String, String>): Double =
                Instant.from(timeFormatter.parse(line.getValue(fieldName)))
                        .toEpochMilli() / 1000.0

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
