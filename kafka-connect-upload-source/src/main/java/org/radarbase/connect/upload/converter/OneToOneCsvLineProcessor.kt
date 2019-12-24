/*
 *  Copyright 2019 The Hyve
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.radarbase.connect.upload.converter

import org.apache.avro.generic.IndexedRecord

/**
 * Simple Processor for one line to one record of a single topic conversion.
 */
class OneToOneCsvLineProcessor(
        override val recordLogger: RecordLogger,
        private val topic: String,
        private val conversion: OneToOneCsvLineProcessor.(lineValues: Map<String, String>, timeReceived: Double) -> IndexedRecord?
) : CsvLineProcessorFactory.CsvLineProcessor {
    override fun convertToRecord(lineValues: Map<String, String>, timeReceived: Double): List<FileProcessorFactory.TopicData>? {
        return conversion(lineValues, timeReceived)?.run {
            listOf(FileProcessorFactory.TopicData(topic, this))
        }
    }
}
