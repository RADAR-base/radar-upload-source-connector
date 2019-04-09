package org.radarbase.upload.doa

import org.radarbase.upload.doa.entity.SourceType

interface SourceTypeRepository {
    fun create(record: SourceType)
    fun read(name: String): SourceType?
    fun readDetailed(name: String): SourceType?
    fun delete(record: SourceType)
    fun update(record: SourceType): SourceType
}
