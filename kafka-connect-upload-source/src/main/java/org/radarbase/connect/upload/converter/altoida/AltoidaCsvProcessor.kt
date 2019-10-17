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

package org.radarbase.connect.upload.converter.altoida

import org.apache.avro.generic.IndexedRecord
import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import org.radarbase.connect.upload.converter.CsvLineProcessorFactory
import org.radarbase.connect.upload.converter.LogRepository
import org.radarbase.connect.upload.converter.SimpleCsvLineProcessor
import org.slf4j.LoggerFactory

abstract class AltoidaCsvProcessor: CsvLineProcessorFactory {
    private val logger = LoggerFactory.getLogger(javaClass)

    abstract val fileNameSuffix: String

    abstract val topic: String

    abstract fun SimpleCsvLineProcessor.lineConversion(line: Map<String, String>, timeReceived: Double): IndexedRecord

    override fun matches(contents: ContentsDTO): Boolean = contents.fileName.endsWith(fileNameSuffix)

    protected fun time(line: Map<String, String>): Double = line.getValue("TIMESTAMP").toDouble() / 1000.0

    override fun csvProcessor(record: RecordDTO, logRepository: LogRepository): CsvLineProcessorFactory.CsvLineProcessor {
        val recordLogger = logRepository.recordLogger(logger, record.id!!)
        return SimpleCsvLineProcessor(recordLogger, topic) { l, t -> lineConversion(l, t) }
    }

}
