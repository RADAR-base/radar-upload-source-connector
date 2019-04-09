package org.radarbase.upload.dto

import java.time.Instant

data class RecordMetadataDTO(var revision: Int, var status: String, var message: String?, var createdDate: Instant, var modifiedDate: Instant, var committedDate: Instant?, var logs: LogsDto? = null)
