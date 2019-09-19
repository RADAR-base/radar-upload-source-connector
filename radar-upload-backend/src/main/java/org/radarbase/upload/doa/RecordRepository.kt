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

package org.radarbase.upload.doa

import org.radarbase.upload.api.ContentsDTO
import org.radarbase.upload.api.Page
import org.radarbase.upload.api.RecordMetadataDTO
import org.radarbase.upload.doa.entity.Record
import org.radarbase.upload.doa.entity.RecordContent
import org.radarbase.upload.doa.entity.RecordLogs
import org.radarbase.upload.doa.entity.RecordMetadata
import java.io.Closeable
import java.io.InputStream
import java.io.Reader

interface RecordRepository {
    fun create(record: Record, metadata: RecordMetadata? = null, contents: Set<ContentsDTO>? = null): Record
    fun read(id: Long): Record?
    fun readLogs(id: Long): RecordLogs?
    fun readLogContents(id: Long): ClobReader?
    fun updateLogs(id: Long, logsData: String): RecordMetadata
    fun delete(record: Record, revision: Int)
    fun readFileContent(id: Long, revision: Int, fileName: String, range: LongRange? = null): BlobReader?
    fun update(record: Record): Record
    fun updateMetadata(id: Long, metadata: RecordMetadataDTO): RecordMetadata
    fun updateContent(record: Record, fileName: String, contentType: String, stream: InputStream, length: Long): RecordContent
    fun readMetadata(id: Long): RecordMetadata?
    fun query(page: Page, projectId: String, userId: String?, status: String?, sourceType: String?): Pair<List<Record>, Long>
    fun poll(limit: Int, supportedConverters: List<String>): List<Record>
    fun readRecordContent(recordId: Long, fileName: String): RecordContent?
    fun deleteContents(record: Record, fileName: String)

    interface BlobReader : Closeable {
        val stream: InputStream
        override fun close()
    }

    interface ClobReader : Closeable {
        val stream: Reader
        override fun close()
    }
}
