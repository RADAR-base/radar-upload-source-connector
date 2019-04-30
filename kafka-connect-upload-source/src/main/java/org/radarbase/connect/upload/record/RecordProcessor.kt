package org.radarbase.connect.upload.record

import io.confluent.connect.avro.AvroData
import org.apache.kafka.connect.data.SchemaAndValue
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.api.UploadBackendClient
import org.radarcns.kafka.ObservationKey
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.lang.IllegalStateException

class RecordProcessor(val record: RecordDTO, val backendClient: UploadBackendClient) {
    private lateinit var observationKey: SchemaAndValue

    fun validateRecord() {
        record.metadata ?: throw IOException("Record meta-data cannot be null")
        record.data ?: throw IOException("Record data cannot be null")
        record.data?.contents ?: throw IOException("Record data has empty content")
    }

    fun getObservationKey(avroData: AvroData): SchemaAndValue {
        if (!::observationKey.isInitialized) {
            observationKey = record.computeObservationKey(avroData)
        }
        return observationKey
    }


    private fun RecordDTO.computeObservationKey(avroData: AvroData): SchemaAndValue {
        val data = this.data
        data ?: throw IllegalStateException("Cannot process record without data")
        return avroData.toConnectData(
                ObservationKey.getClassSchema(),
                ObservationKey(
                        data.projectId,
                        data.userId,
                        data.sourceId
                )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RecordProcessor::class.java)
    }
}
