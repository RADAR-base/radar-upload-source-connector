package org.radarbase.connect.upload.converter.axivity

import org.radarbase.connect.upload.converter.RecordLogger
import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.axivity.newcastle.CwaBlock
import org.radarcns.connector.upload.axivity.AxivityAcceleration

class AccelerationCwaBlockProcessor: CwaFileProcessorFactory.CwaBlockProcessor {
    override fun processBlock(recordLogger: RecordLogger, block: CwaBlock, timeReceived: Double): Sequence<TopicData> {
        return (0 until block.numSamples)
            .asSequence()
                .map { i -> TopicData("connect_upload_axivity_acceleration",
                        AxivityAcceleration(
                                block.timestampValues[i] / 1000.0,
                                timeReceived,
                                block.sampleValues[CwaBlock.NUM_AXES_PER_SAMPLE * i].toFloat() / 256.0f,
                                block.sampleValues[CwaBlock.NUM_AXES_PER_SAMPLE * i + 1].toFloat() / 256.0f,
                                block.sampleValues[CwaBlock.NUM_AXES_PER_SAMPLE * i + 2].toFloat() / 256.0f))
                }
    }
}
