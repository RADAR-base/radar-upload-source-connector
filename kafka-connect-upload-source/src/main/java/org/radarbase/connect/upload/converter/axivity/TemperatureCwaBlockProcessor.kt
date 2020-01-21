package org.radarbase.connect.upload.converter.axivity

import org.radarbase.connect.upload.converter.RecordLogger
import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.axivity.newcastle.CwaBlock
import org.radarcns.connector.upload.axivity.AxivityTemperature

class TemperatureCwaBlockProcessor: CwaFileProcessorFactory.CwaBlockProcessor {
    override fun processBlock(recordLogger: RecordLogger, block: CwaBlock, timeReceived: Double): List<TopicData> {
        return listOf(TopicData(
                "connect_upload_axivity_temperature",
                AxivityTemperature(
                        block.startTime(),
                        timeReceived,
                        ((block.temperature.toInt() * 3 - 410) / 20.0f))))
    }
}
