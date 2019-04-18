package org.radarbase.upload.api

import org.radarbase.upload.doa.entity.SourceType

class SourceTypeMapperImpl: SourceTypeMapper {

    override fun fromSourceType(record: SourceType) = SourceTypeDTO(
            name = record.name,
            topics = record.topics,
            contentTypes = record.contentTypes,
            timeRequired = record.timeRequired,
            sourceIdRequired = record.sourceIdRequired,
            configuration = record.configuration
    )

    override fun fromSourceTypes(records: List<SourceType>) = SourceTypeContainerDTO(
            sourceTypes = records.map (::fromSourceType)
    )
}
