package org.radarbase.connect.upload.converter.axivity

import org.radarbase.connect.upload.logging.RecordLogger
import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.axivity.newcastle.CwaBlock
import org.radarcns.connector.upload.axivity.AxivityBatteryLevel

class BatteryLevelCwaBlockProcessor: CwaFileProcessorFactory.CwaBlockProcessor {
    override fun processBlock(
        recordLogger: RecordLogger,
        block: CwaBlock,
        timeReceived: Double,
    ): Sequence<TopicData> {
        return sequenceOf(TopicData(
                "connect_upload_axivity_battery_level",
                AxivityBatteryLevel(
                        block.startTime,
                        timeReceived,
                        (block.battery.toInt() + 512) * 3 / 512.0f)))
    }
}
