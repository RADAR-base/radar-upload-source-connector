package org.radarbase.connect.upload.converter.axivity

import org.radarbase.connect.upload.converter.RecordLogger
import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.axivity.newcastle.CwaBlock
import org.radarcns.connector.upload.axivity.AxivityBattery

class BatteryLevelCwaBlockProcessor: CwaFileProcessorFactory.CwaBlockProcessor {
    override fun processBlock(recordLogger: RecordLogger, block: CwaBlock, timeReceived: Double): List<TopicData> {
        return listOf(TopicData(
                "connect_upload_axivity_battery_level",
                AxivityBattery(
                        block.timestampValues[0] / 1000.0,
                        timeReceived,
                        block.battery.toFloat()
                )
        ))
    }
}
