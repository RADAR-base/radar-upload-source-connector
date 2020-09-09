package org.radarbase.connect.upload.converter.axivity

import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.axivity.newcastle.CwaReader
import org.radarcns.connector.upload.axivity.AxivityMetadata

class MetadataCwaReaderProcessor {
    fun processReader(cwaReader: CwaReader, time: Double, timeReceived: Double): Sequence<TopicData> {
        val records = mutableListOf<AxivityMetadata>()

        cwaReader.deviceId.takeUnless { it == -1 }
                ?.let {
                    records += AxivityMetadata(time, timeReceived, "deviceId", it.toString())
                }

        cwaReader.sessionId.takeUnless { it == -1 }
                ?.let {
                    records += AxivityMetadata(time, timeReceived, "sessionId", it.toString())
                }

        records += cwaReader.annotations.map {
            AxivityMetadata(time, timeReceived, it.key, it.value)
        }

        return records.asSequence().map { TopicData("connect_upload_axivity_metadata", it) }
    }
}
