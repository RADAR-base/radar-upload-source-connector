package org.radarbase.connect.upload.converter.axivity

import okhttp3.internal.and
import org.radarbase.connect.upload.converter.RecordLogger
import org.radarbase.connect.upload.converter.TopicData
import org.radarbase.connect.upload.converter.axivity.newcastle.CwaBlock

class EventsCwaBlockProcessor: CwaFileProcessorFactory.CwaBlockProcessor {
    override fun processBlock(recordLogger: RecordLogger, block: CwaBlock, timeReceived: Double): List<TopicData> {
        if (block.events == 0.toShort()) {
            recordLogger.info("No block events")
        } else {
            if (block.events and CwaBlock.DATA_EVENT_RESUME != 0) {
                recordLogger.info("Block event: resume")
            }
            if (block.events and CwaBlock.DATA_EVENT_SINGLE_TAP != 0) {
                recordLogger.info("Block event: single tap")
            }
            if (block.events and CwaBlock.DATA_EVENT_DOUBLE_TAP != 0) {
                recordLogger.info("Block event: double tap")
            }
            if (block.events and CwaBlock.DATA_EVENT_EVENT != 0) {
                recordLogger.info("Block event: event")
            }
            if (block.events and CwaBlock.DATA_EVENT_FIFO_OVERFLOW != 0) {
                recordLogger.info("Block event: FIFO overflow")
            }
            if (block.events and CwaBlock.DATA_EVENT_BUFFER_OVERFLOW != 0) {
                recordLogger.info("Block event: buffer overflow")
            }
            if (block.events and CwaBlock.DATA_EVENT_UNHANDLED_INTERRUPT != 0) {
                recordLogger.info("Block event: unhandled interrupt")
            }
            if (block.events and CwaBlock.DATA_EVENT_CHECKSUM_FAIL != 0) {
                recordLogger.info("Block event: checksum fail")
            }
        }
        return emptyList()
    }
}
