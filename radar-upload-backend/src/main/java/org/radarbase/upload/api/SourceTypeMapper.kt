package org.radarbase.upload.api

import org.radarbase.upload.doa.entity.SourceType

interface SourceTypeMapper {
    fun fromSourceType(sourceType: SourceType): SourceTypeDTO
    fun fromSourceTypes(sourceTypes: List<SourceType>): SourceTypeContainerDTO

    fun toSourceType(sourceType: SourceTypeDTO): SourceType
}
