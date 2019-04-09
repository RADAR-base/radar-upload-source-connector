package org.radarbase.upload.doa

import org.radarbase.upload.doa.entity.Record
import org.radarbase.upload.doa.entity.RecordContent
import org.radarbase.upload.doa.entity.RecordLogs
import org.radarbase.upload.doa.entity.RecordMetadata
import java.io.Closeable
import java.io.InputStream
import java.io.Reader
import java.sql.Blob
import java.sql.Clob

interface RecordRepository {
    fun create(record: Record): Record
    fun read(id: Long): Record?
    fun readLogs(id: Long): RecordLogs?
    fun updateLogs(id: Long, reader: Reader, length: Long)
    fun readContent(id: Long): RecordContent?
    fun delete(record: Record)
    fun update(record: Record): Record
    fun update(metadata: RecordMetadata): RecordMetadata
    fun updateContent(record: Record, fileName: String, contentType: String, stream: InputStream, length: Long): RecordContent
    fun readMetadata(id: Long): RecordMetadata?
    fun query(limit: Int, lastId: Long, projectId: String, userId: String?, status: String?): List<Record>
    fun poll(limit: Int): List<Record>
}
