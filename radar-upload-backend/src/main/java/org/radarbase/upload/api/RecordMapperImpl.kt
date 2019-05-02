package org.radarbase.upload.api

import org.radarbase.upload.doa.SourceTypeRepository
import org.radarbase.upload.doa.entity.Record
import org.radarbase.upload.doa.entity.RecordContent
import org.radarbase.upload.doa.entity.RecordMetadata
import java.time.Instant
import javax.ws.rs.BadRequestException
import javax.ws.rs.core.Context
import javax.ws.rs.core.UriInfo

class RecordMapperImpl : RecordMapper {
    @Context
    lateinit var uri: UriInfo

    @Context
    lateinit var sourceTypeRepository: SourceTypeRepository

    override fun toRecord(record: RecordDTO): Record = Record().apply {
        val data = record.data ?: throw BadRequestException("No data field included")
        metadata = toMetadata(record.metadata)
        projectId = data.projectId ?: throw BadRequestException("Missing project ID")
        userId = data.userId ?: throw BadRequestException("Missing user ID")
        sourceId = data.sourceId ?: throw BadRequestException("Missing source ID")
        sourceType = sourceTypeRepository.read(record.sourceType
                ?: throw BadRequestException("Missing source type"))
                ?: throw BadRequestException("Source type not found")
    }

    fun toMetadata(metadata: RecordMetadataDTO?) = RecordMetadata().apply {
        createdDate = Instant.now()
        modifiedDate = Instant.now()
        revision = 1

        callbackUrl = metadata?.callbackUrl
    }

    override fun fromRecord(record: Record) = RecordDTO(
            id = record.id!!,
            metadata = fromMetadata(record.metadata).apply { id = null },
            sourceType = record.sourceType.name,
            data = RecordDataDTO(
                    projectId = record.projectId,
                    userId = record.userId,
                    sourceId = record.sourceId,
                    time = record.time,
                    timeZoneOffset = record.timeZoneOffset,
                    contents = record.contents?.mapTo(HashSet(), this::fromContent)
            ))

    override fun fromRecords(records: List<Record>, limit: Int) = RecordContainerDTO(
            limit = limit,
            records = records.map(::fromRecord))

    override fun fromContent(content: RecordContent) = ContentsDTO(
            url = "${uri.baseUri}/records/${content.record.id}/contents/${content.fileName}",
            contentType = content.contentType,
            createdDate = content.createdDate,
            size = content.size,
            fileName = content.fileName)

    override fun fromMetadata(metadata: RecordMetadata) = RecordMetadataDTO(
            id = metadata.id,
            revision = metadata.revision,
            status = metadata.status.name,
            message = metadata.message,
            createdDate = metadata.createdDate,
            modifiedDate = metadata.modifiedDate,
            committedDate = metadata.committedDate,
            // use record.id, since metadata and record have one-to-one
            logs = metadata.logs?.let {
                LogsDto(url = "${uri.baseUri}/records/${metadata.id}/logs")
            }
    )
}
