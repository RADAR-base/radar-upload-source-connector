package org.radarbase.connect.upload.converter

import org.apache.avro.generic.IndexedRecord

/** A single data record destined for given topic. */
data class TopicData(
    val topic: String,
    val value: IndexedRecord,
)
