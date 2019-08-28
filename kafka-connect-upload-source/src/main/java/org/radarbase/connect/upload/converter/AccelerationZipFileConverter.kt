/*
 *
 *  * Copyright 2019 The Hyve
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */

package org.radarbase.connect.upload.converter

import org.slf4j.LoggerFactory

class AccelerationZipFileConverter(override val sourceType: String = "acceleration-zip") : ZipFileRecordConverter(sourceType) {

    override fun getProcessors() : Map<String, DataProcessor> {
        logger.info("Number of registered data Processors ${processors.size}")
        return processors
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AccelerationZipFileConverter::class.java)
        private val processors = listOf<DataProcessor>(
                AccelerometerCsvProcessor()
        ).map { it.schemaType to it }.toMap()
    }

}
