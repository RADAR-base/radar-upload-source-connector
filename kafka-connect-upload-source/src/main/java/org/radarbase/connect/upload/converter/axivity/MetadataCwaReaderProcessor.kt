package org.radarbase.connect.upload.converter.axivity

import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.axivity.newcastle.CwaReader
import org.radarcns.connector.upload.axivity.AxivityMetadata
import java.util.AbstractMap

class MetadataCwaReaderProcessor {
    fun processReader(cwaReader: CwaReader, time: Double, timeReceived: Double): Sequence<TopicData> =
        sequence {
            if (cwaReader.deviceId != -1) {
                yield(AbstractMap.SimpleImmutableEntry("deviceId", cwaReader.deviceId.toString()))
            }

            if (cwaReader.sessionId != -1) {
                yield(AbstractMap.SimpleImmutableEntry("sessionId", cwaReader.sessionId.toString()))
            }

            yieldAll(cwaReader.annotations.entries)
        }.map { (annotationName, annotationData) ->
            TopicData(
                "connect_upload_axivity_metadata",
                AxivityMetadata(time, timeReceived, annotationName, annotationData),
            )
        }
}
