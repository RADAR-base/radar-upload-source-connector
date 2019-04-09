package org.radarbase.upload.dto

import org.radarbase.upload.doa.entity.Record
import org.radarbase.upload.doa.entity.RecordMetadata

class RecordMapperImpl: RecordMapper {
    override fun toRecord(record: RecordDTO): Record {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun fromRecord(record: Record) = RecordDTO(
            id = record.id!!,
            metadata = fromMetadata(record.metadata),
            sourceType = record.sourceType.name,
            data = RecordDataDTO(
                    projectId = record.projectId,
                    userId = record.userId,
                    sourceId = record.sourceId,
                    time = record.time,
                    timeZoneOffset = record.timeZoneOffset,
                    contents = record.contents?.mapTo(HashSet()) {
                        ContentsDTO(
                                url = "/records/${record.id}/contents/${it.fileName}",
                                contentType = it.contentType,
                                createdDate = it.createdDate,
                                size = it.size)
                    }
            ))

    override fun fromRecords(records: List<Record>, limit: Int) = RecordContainerDTO(
            limit = limit,
            records = records.map(::fromRecord))

    override fun fromMetadata(metadata: RecordMetadata) = RecordMetadataDTO(
            revision = metadata.revision,
            status = metadata.status.name,
            message = metadata.message,
            createdDate = metadata.createdDate,
            modifiedDate = metadata.modifiedDate,
            committedDate = metadata.committedDate,
            logs = metadata.logs?.let {
                LogsDto(url = "/records/${metadata.id}/logs")
            }
    )
}
