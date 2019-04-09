package org.radarbase.upload.doa

import org.hibernate.Hibernate
import org.hibernate.Session
import org.radarbase.upload.doa.entity.*
import org.radarbase.upload.inject.session
import org.radarbase.upload.inject.transact
import java.io.InputStream
import java.io.Reader
import java.time.Instant
import javax.persistence.EntityManager
import javax.ws.rs.NotFoundException
import javax.ws.rs.core.Context


class RecordRepositoryImpl(@Context private var em: EntityManager) : RecordRepository {
    override fun updateLogs(id: Long, reader: Reader, length: Long): Unit = em.transact {
        val logs = find(RecordLogs::class.java, id)
        if (logs == null) {
            val metadata = find(RecordMetadata::class.java, id)  ?: throw NotFoundException("RecordMetadata with ID $id does not exist")
            metadata.logs = RecordLogs().apply {
                this.id = id
                this.metadata = metadata
                this.modifiedDate = Instant.now()
                this.size = length
                this.logs = Hibernate.getLobCreator(em.session).createClob(reader, length)
            }
            persist(metadata.logs!!)
            merge(metadata)
        } else {
            logs.apply {
                this.modifiedDate = Instant.now()
                this.logs = Hibernate.getLobCreator(em.session).createClob(reader, length)
            }
            merge(logs)
        }
    }

    override fun query(limit: Int, lastId: Long, projectId: String, userId: String?, status: String?): List<Record> {
        var queryString = "SELECT r FROM Record r WHERE r.projectId = :projectId AND r.id > :lastId"
        userId?.let {
            queryString += " AND r.userId = :userId"
        }
        status?.let {
            queryString += " AND r.metadata.status = :status"
        }
        queryString += " ORDER BY r.id"

        return em.transact {
            val query = createQuery(queryString, Record::class.java)
                    .setParameter("lastId", lastId)
                    .setParameter("projectId", projectId)
                    .setMaxResults(limit)
            userId?.let {
                query.setParameter("userId", it)
            }
            status?.let {
                query.setParameter("status", it)
            }
            query.resultList
        }
    }

    override fun readLogs(id: Long): RecordLogs? = em.transact {
        find(RecordLogs::class.java, id)
    }

    override fun updateContent(record: Record, fileName: String, contentType: String, stream: InputStream, length: Long): Unit = em.transact {
        val existingContent = record.content

        if (existingContent == null) {
            val newContent = RecordContent().apply {
                this.record = record
                id = record.id
                this.fileName = fileName
                this.createdDate = Instant.now()
                this.contentType = contentType
                this.size = length
                this.content = Hibernate.getLobCreator(em.unwrap(Session::class.java)).createBlob(stream, length)
            }
            record.content = newContent

            persist(newContent)
            merge(record)
        } else {
            existingContent.apply {
                this.fileName = fileName
                this.createdDate = Instant.now()
                this.contentType = contentType
                this.content = Hibernate.getLobCreator(em.unwrap(Session::class.java)).createBlob(stream, length)
            }
            merge(existingContent)
        }
    }

    override fun readContent(id: Long): RecordContent? = em.transact {
        find(RecordContent::class.java, id)
    }

    override fun create(record: Record): Record = em.transact {
        val tmpContent = record.content?.also { record.content = null }

        persist(record)

        tmpContent?.let {
            it.record = record
            it.createdDate = Instant.now()
            persist(it)
            record.content = it
        }

        val metadata = RecordMetadata().apply {
            this.record = record
            status = if (tmpContent == null) RecordStatus.INCOMPLETE else RecordStatus.READY
            message = if (tmpContent == null) "No data uploaded yet" else "Data successfully uploaded, ready for processing."
            createdDate = Instant.now()
            modifiedDate = Instant.now()
            revision = 1
        }

        persist(metadata)

        record.apply {
            this.content = content
            this.metadata = metadata
        }
    }

    override fun read(id: Long): Record? = em.transact { find(Record::class.java, id) }

    override fun update(record: Record): Record = em.transact { merge<Record>(record) }

    override fun update(metadata: RecordMetadata): RecordMetadata = em.transact { merge<RecordMetadata>(metadata) }

    override fun delete(record: Record) = em.transact { remove(record) }

    override fun readMetadata(id: Long): RecordMetadata? = em.transact { find(RecordMetadata::class.java, id) }
}
