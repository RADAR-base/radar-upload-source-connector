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

import org.radarbase.connect.upload.api.ContentsDTO
import org.radarbase.connect.upload.api.RecordDTO
import java.io.InputStream

/**
 * Factory to create processors of single files.
 */
interface FileProcessorFactory {

    /**
     * Whether this processor factory can process given file contents. Matching could occur based on
     * file name, size or content type.
     */
    fun matches(contents: ContentsDTO): Boolean

    /**
     * Create a file processor for a given record.
     */
    fun createProcessor(record: RecordDTO): FileProcessor

    /** Processor to process a single file in a record. */
    interface FileProcessor {
        fun processData(
                contents: ContentsDTO,
                inputStream: InputStream,
                timeReceived: Double
        ): Sequence<TopicData>
    }
}
