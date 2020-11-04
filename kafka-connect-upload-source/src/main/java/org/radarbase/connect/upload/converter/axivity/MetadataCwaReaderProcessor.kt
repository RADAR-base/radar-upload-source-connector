package org.radarbase.connect.upload.converter.axivity

import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.axivity.newcastle.CwaReader
import org.radarcns.connector.upload.axivity.AxivityMetadata
import java.util.AbstractMap

class MetadataCwaReaderProcessor {
    fun processReader(cwaReader: CwaReader, time: Double, timeReceived: Double): Sequence<TopicData> {
        val records = mutableListOf<Map.Entry<String, String>>()

        if (cwaReader.deviceId != -1) {
            records += AbstractMap.SimpleImmutableEntry("deviceId", cwaReader.deviceId.toString())
        }

        if (cwaReader.sessionId != -1) {
            records += AbstractMap.SimpleImmutableEntry("sessionId", cwaReader.sessionId.toString())
        }

        records += cwaReader.annotations.entries

        return records.asSequence()
                .map { TopicData("connect_upload_axivity_metadata",
                        AxivityMetadata(time, timeReceived, it.key, it.value))
                }
    }
}
