package org.radarbase.upload.dto

import java.time.Instant
import java.time.LocalDateTime

data class LogsDto(
        var url: String? = null,
        var contents: String? = null)

data class ContentsDTO(
        var url: String? = null,
        var text: String? = null,
        var contentType: String,
        var createdDate: Instant,
        var size: Long,
        var fileName: String)

data class RecordDataDTO(
        var projectId: String?,
        var userId: String?,
        var sourceId: String?,
        var time: LocalDateTime? = null,
        var timeZoneOffset: Int? = null,
        var contents: Set<ContentsDTO>? = null)

data class RecordMetadataDTO(
        var revision: Int,
        var status: String,
        var message: String?,
        var createdDate: Instant?,
        var modifiedDate: Instant?,
        var committedDate: Instant?,
        var logs: LogsDto? = null)


data class RecordDTO(
        var id: Long?,
        var data: RecordDataDTO?,
        var sourceType: String?,
        var metadata: RecordMetadataDTO?)

data class RecordContainerDTO(
        var limit: Int? = null,
        var records: List<RecordDTO>)

data class SourceTypeDTO(
        var name: String?,
        var topics: Set<String>?,
        var contentTypes: Set<String>?,
        var timeRequired: Boolean?,
        var sourceIdRequired: Boolean?,
        var configuration: Map<String, String>?
        )

data class SourceTypeContainerDTO(
        var sourceTypes: List<SourceTypeDTO>)
