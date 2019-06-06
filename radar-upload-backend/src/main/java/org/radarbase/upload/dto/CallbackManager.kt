package org.radarbase.upload.dto

import org.radarbase.upload.api.RecordMetadataDTO

interface CallbackManager {
    fun callback(metadata: RecordMetadataDTO, retries: Int = 10)
}
