package org.radarbase.upload.api

import org.radarbase.upload.doa.entity.SourceType

class SourceTypeMapperImpl: SourceTypeMapper {

    override fun fromSourceType(sourceType: SourceType) = SourceTypeDTO(
            name = sourceType.name,
            topics = sourceType.topics,
            contentTypes = sourceType.contentTypes,
            timeRequired = sourceType.timeRequired,
            sourceIdRequired = sourceType.sourceIdRequired,
            configuration = sourceType.configuration
    )

    override fun fromSourceTypes(sourceTypes: List<SourceType>) = SourceTypeContainerDTO(
            sourceTypes = sourceTypes.map (::fromSourceType)
    )

    override fun toSourceType(sourceType: SourceTypeDTO): SourceType {
        val entity = SourceType()
        entity.name = sourceType.name
        entity.topics = sourceType.topics ?: emptySet()
        entity.contentTypes = sourceType.contentTypes ?: emptySet()
        entity.sourceIdRequired = sourceType.sourceIdRequired ?: false
        entity.timeRequired = sourceType.timeRequired ?: false
        entity.configuration = sourceType.configuration ?: emptyMap()
        return entity
    }

}
