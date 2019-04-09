package org.radarbase.upload.dto

import java.time.LocalDateTime

data class RecordDataDTO(
        var projectId: String?,
        var userId: String?,
        var sourceId: String?,
        var time: LocalDateTime? = null,
        var timeZoneOffset: Int? = null,
        var contents: Set<ContentsDTO>? = null)
