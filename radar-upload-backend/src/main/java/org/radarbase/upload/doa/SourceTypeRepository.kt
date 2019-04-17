package org.radarbase.upload.doa

import org.radarbase.upload.doa.entity.SourceType

interface SourceTypeRepository {
    fun create(record: SourceType)
    fun read(name: String): SourceType?
    fun readAll(limit: Int? = 20, lastId: Long? = -1L, detailed: Boolean = false): List<SourceType>
    fun delete(record: SourceType)
    fun update(record: SourceType): SourceType
}
