package org.radarbase.connect.upload.converter.axivity

import org.radarbase.connect.upload.converter.RecordLogger
import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.axivity.newcastle.CwaBlock
import org.radarcns.connector.upload.axivity.AxivityLight
import java.lang.Math.pow
import kotlin.math.pow

class LightCwaBlockProcessor: CwaFileProcessorFactory.CwaBlockProcessor {
    override fun processBlock(recordLogger: RecordLogger, block: CwaBlock, timeReceived: Double): List<TopicData> {
        return listOf(TopicData(
                "connect_upload_axivity_light",
                AxivityLight(
                        block.startTime(),
                        timeReceived,
                        10.0.pow((block.light.toInt() + 512) * 3 / 512.0).toFloat())))
    }
}