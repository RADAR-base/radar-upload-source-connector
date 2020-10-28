package org.radarbase.connect.upload.converter.axivity

import okhttp3.internal.and
import org.radarbase.connect.upload.converter.RecordLogger
import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.axivity.newcastle.CwaBlock
import org.radarcns.connector.upload.axivity.AxivityEvent
import org.radarcns.connector.upload.axivity.AxivityEventType
import org.radarcns.connector.upload.axivity.AxivityEventType.*

class EventsCwaBlockProcessor: CwaFileProcessorFactory.CwaBlockProcessor {
    override fun processBlock(recordLogger: RecordLogger, block: CwaBlock, timeReceived: Double): List<TopicData> {
        return if (block.events == 0.toShort()) {
            recordLogger.debug("No block events")
            emptyList()
        } else {
            AxivityEventType.values()
                    .filter { block.events and it.toBitPattern() != 0 }
                    .map { TopicData("connect_upload_axivity_event", AxivityEvent(
                            block.startTime(),
                            timeReceived,
                            it)) }
        }
    }

    companion object {
        private fun AxivityEventType.toBitPattern(): Int = when(this) {
            RESUME -> CwaBlock.DATA_EVENT_RESUME
            SINGLE_TAP -> CwaBlock.DATA_EVENT_SINGLE_TAP
            DOUBLE_TAP -> CwaBlock.DATA_EVENT_DOUBLE_TAP
            FIFO_OVERFLOW -> CwaBlock.DATA_EVENT_FIFO_OVERFLOW
            BUFFER_OVERFLOW -> CwaBlock.DATA_EVENT_BUFFER_OVERFLOW
            UNHANDLED_INTERRUPT -> CwaBlock.DATA_EVENT_UNHANDLED_INTERRUPT
            // all bits except the already handled ones
            UNKNOWN -> (CwaBlock.DATA_EVENT_RESUME
                    or CwaBlock.DATA_EVENT_SINGLE_TAP
                    or CwaBlock.DATA_EVENT_DOUBLE_TAP
                    or CwaBlock.DATA_EVENT_FIFO_OVERFLOW
                    or CwaBlock.DATA_EVENT_BUFFER_OVERFLOW
                    or CwaBlock.DATA_EVENT_UNHANDLED_INTERRUPT).inv()
        }
    }
}
