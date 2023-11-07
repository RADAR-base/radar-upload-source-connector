package org.radarbase.connect.upload.converter

import org.apache.avro.generic.IndexedRecord
import org.apache.kafka.connect.source.SourceRecord

/** A single data record destined for given topic. */
data class TopicData(
    val topic: String,
    val value: IndexedRecord,
) {
    fun toSourceRecord(
        context: ConverterFactory.ContentsContext,
        partition: Map<String, Any>,
    ): SourceRecord {
        try {
            val valRecord = context.avroData.toConnectData(value.schema, value)
            val offset = mutableMapOf(
                ConverterFactory.Converter.END_OF_RECORD_KEY to false,
                ConverterFactory.Converter.RECORD_ID_KEY to context.id,
                ConverterFactory.Converter.REVISION_KEY to context.metadata.revision,
            )
            return SourceRecord(
                partition,
                offset,
                topic,
                context.key.schema(),
                context.key.value(),
                valRecord.schema(),
                valRecord.value(),
            )
        } catch (exe: Exception) {
            context.logger.info("This value $value and schema ${value.schema.toString(true)} could not be converted")
            throw exe
        }
    }
}
