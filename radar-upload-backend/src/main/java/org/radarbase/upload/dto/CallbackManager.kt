package org.radarbase.upload.dto

import org.radarbase.upload.doa.entity.RecordMetadata

interface CallbackManager {
    fun callback(metadata: RecordMetadataDTO, retries: Int = 10)
}
