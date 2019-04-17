package org.radarbase.upload.dto

import org.radarbase.upload.doa.entity.SourceType

interface SourceTypeMapper {
    fun fromSourceType(record: SourceType): SourceTypeDTO
    fun fromSourceTypes(records: List<SourceType>): SourceTypeContainerDTO
}
