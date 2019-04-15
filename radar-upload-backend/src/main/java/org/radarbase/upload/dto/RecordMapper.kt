package org.radarbase.upload.dto

import org.radarbase.upload.doa.entity.Record
import org.radarbase.upload.doa.entity.RecordContent
import org.radarbase.upload.doa.entity.RecordMetadata

interface RecordMapper {
    fun fromRecord(record: Record): RecordDTO
    fun fromRecords(records: List<Record>, limit: Int): RecordContainerDTO
    fun fromMetadata(metadata: RecordMetadata): RecordMetadataDTO
    fun fromContent(content: RecordContent): ContentsDTO

    fun toRecord(record: RecordDTO): Record
    fun toMetadata(metadata: RecordMetadataDTO, origin: RecordMetadata): RecordMetadata

    fun fromMetadataToLogs(metadata: RecordMetadata): LogsDto

}
