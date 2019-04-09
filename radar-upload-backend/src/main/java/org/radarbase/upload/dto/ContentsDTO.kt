package org.radarbase.upload.dto

import java.time.Instant

data class ContentsDTO(var url: String? = null, var text: String? = null, var contentType: String, var createdDate: Instant, var size: Long)
