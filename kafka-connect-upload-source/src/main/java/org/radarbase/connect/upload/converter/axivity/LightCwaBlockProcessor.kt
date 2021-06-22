package org.radarbase.connect.upload.converter.axivity

import org.radarbase.connect.upload.logging.RecordLogger
import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.axivity.newcastle.CwaBlock
import org.radarcns.connector.upload.axivity.AxivityLight
import kotlin.math.pow

class LightCwaBlockProcessor: CwaFileProcessorFactory.CwaBlockProcessor {
    override fun processBlock(
        recordLogger: RecordLogger,
        block: CwaBlock,
        timeReceived: Double,
    ): Sequence<TopicData> {
        return sequenceOf(TopicData(
                "connect_upload_axivity_light",
                AxivityLight(
                        block.startTime,
                        timeReceived,
                        10.0.pow((block.light.toInt() + 512) * 3 / 512.0).toFloat())))
    }
}
