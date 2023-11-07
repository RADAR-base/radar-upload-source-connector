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

package org.radarbase.upload.api

import org.radarbase.upload.doa.entity.Record
import org.radarbase.upload.doa.entity.RecordContent
import org.radarbase.upload.doa.entity.RecordMetadata

interface RecordMapper {
    suspend fun cleanBaseUri(): String

    suspend fun fromRecord(record: Record): RecordDTO
    suspend fun fromRecords(records: List<Record>, page: Page?): RecordContainerDTO
    suspend fun fromMetadata(metadata: RecordMetadata): RecordMetadataDTO
    suspend fun fromContent(content: RecordContent): ContentsDTO

    suspend fun toRecord(record: RecordDTO): Pair<Record, RecordMetadata>
}
