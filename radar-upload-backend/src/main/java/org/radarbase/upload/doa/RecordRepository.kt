package org.radarbase.upload.doa

import org.radarbase.upload.doa.entity.Record
import org.radarbase.upload.doa.entity.RecordContent
import org.radarbase.upload.doa.entity.RecordLogs
import org.radarbase.upload.doa.entity.RecordMetadata
import org.radarbase.upload.dto.RecordMetadataDTO
import java.io.InputStream
import java.io.Reader
import java.sql.Blob

interface RecordRepository {
    fun create(record: Record): Record
    fun read(id: Long): Record?
    fun readLogs(id: Long): RecordLogs?
    fun updateLogs(id: Long, reader: Reader, length: Long)
    fun readContent(id: Long, fileName: String): RecordContent?
    fun delete(record: Record)
    fun update(record: Record): Record
    fun updateMetadata(id: Long, metadata: RecordMetadataDTO): RecordMetadata
    fun updateContent(record: Record, fileName: String, contentType: String, stream: InputStream, length: Long): RecordContent
    fun readMetadata(id: Long): RecordMetadata?
    fun query(limit: Int, lastId: Long, projectId: String, userId: String?, status: String?): List<Record>
    fun poll(limit: Int): List<Record>
}
