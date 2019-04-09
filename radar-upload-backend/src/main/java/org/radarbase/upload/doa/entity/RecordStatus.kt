package org.radarbase.upload.doa.entity

enum class RecordStatus {
    UPLOADING,
    INCOMPLETE,
    READY,
    QUEUED,
    PROCESSING,
    FAILED,
    SUCCEEDED
}
