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

import java.time.Instant
import java.time.LocalDateTime

data class LogsDto(
        var url: String? = null,
        var contents: String? = null)

data class ContentsDTO(
        var url: String? = null,
        var text: String? = null,
        var contentType: String,
        var createdDate: Instant? = null,
        var size: Long? = null,
        var fileName: String)

data class RecordDataDTO(
        var projectId: String?,
        var userId: String?,
        var sourceId: String?,
        var time: LocalDateTime? = null,
        var timeZoneOffset: Int? = null,
        var contents: Set<ContentsDTO>? = null)

data class RecordMetadataDTO(
        var id: Long? = null,
        var revision: Int? = null,
        var status: String? = null,
        var message: String? = null,
        var createdDate: Instant? = null,
        var modifiedDate: Instant? = null,
        var committedDate: Instant? = null,
        var logs: LogsDto? = null,
        var callbackUrl: String? = null)

data class RecordDTO(
        var id: Long?,
        var data: RecordDataDTO?,
        var sourceType: String?,
        var metadata: RecordMetadataDTO?)

data class RecordContainerDTO(
        var limit: Int? = null,
        var records: List<RecordDTO>,
        var lastId: Long? = null)

data class SourceTypeDTO(
        var name: String,
        var topics: Set<String>?,
        var contentTypes: Set<String>?,
        var timeRequired: Boolean?,
        var sourceIdRequired: Boolean?,
        var configuration: Map<String, String>?
)

data class SourceTypeContainerDTO(
        var sourceTypes: List<SourceTypeDTO>)

data class PollDTO(
        var limit: Int = 10,
        var supportedConverters: List<String>)
