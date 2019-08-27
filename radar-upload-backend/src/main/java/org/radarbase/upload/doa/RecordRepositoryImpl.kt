/*
 *
 *  * Copyright 2019 The Hyve
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */

package org.radarbase.upload.doa

import org.hibernate.Hibernate
import org.hibernate.Session
import org.radarbase.upload.api.RecordMetadataDTO
import org.radarbase.upload.doa.entity.*
import org.radarbase.upload.exception.ConflictException
import org.radarbase.upload.inject.session
import org.radarbase.upload.inject.transact
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.sql.Blob
import java.time.Instant
import javax.persistence.EntityManager
import javax.persistence.LockModeType
import javax.persistence.PessimisticLockScope
import javax.ws.rs.NotFoundException
import javax.ws.rs.core.Context
import kotlin.collections.HashSet
import kotlin.streams.toList


class RecordRepositoryImpl(@Context private var em: javax.inject.Provider<EntityManager>) : RecordRepository {

    override fun updateLogs(id: Long, logsData: String): RecordMetadata = em.get().transact {
        val logs = find(RecordLogs::class.java, id)
        val metadataToSave = if (logs == null) {
            val metadataFromDb = find(RecordMetadata::class.java, id)
                    ?: throw NotFoundException("RecordMetadata with ID $id does not exist")
            refresh(metadataFromDb)
            metadataFromDb.logs = RecordLogs().apply {
                this.metadata = metadataFromDb
                this.modifiedDate = Instant.now()
                this.size = logsData.length.toLong()
                this.logs = Hibernate.getLobCreator(em.get().session).createClob(logsData)
            }.also {
                persist(it)
            }
            metadataFromDb
        } else {
            logs.apply {
                this.modifiedDate = Instant.now()
                this.logs = Hibernate.getLobCreator(em.get().session).createClob(logsData)
            }
            merge(logs)
            logs.metadata
        }

        modifyNow(metadataToSave)
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

        return em.get().transact {
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

    override fun readLogs(id: Long): RecordLogs? = em.get().transact {
        find(RecordLogs::class.java, id)
    }

    override fun updateContent(record: Record, fileName: String, contentType: String, stream: InputStream, length: Long): RecordContent = em.get().transact {
        val existingContent = record.contents?.find { it.fileName == fileName }

        val result = existingContent?.apply {
            this.createdDate = Instant.now()
            this.contentType = contentType
            this.content = Hibernate.getLobCreator(em.get().unwrap(Session::class.java)).createBlob(stream, length)
        } ?: RecordContent().apply {
            this.record = record
            this.fileName = fileName
            this.createdDate = Instant.now()
            this.contentType = contentType
            this.size = length
            this.content = Hibernate.getLobCreator(em.get().unwrap(Session::class.java)).createBlob(stream, length)
        }.also {
            if (record.contents != null) {
                record.contents?.add(it)
            } else {
                record.contents = mutableSetOf(it)
            }
        }

        record.metadata.apply {
            status = RecordStatus.READY
            message = "Data successfully uploaded, ready for processing."
            update()
        }

        merge(record)
        return@transact result
    }

    override fun poll(limit: Int): List<Record> = em.get().transact {
        setProperty("javax.persistence.lock.scope", PessimisticLockScope.EXTENDED)
        createQuery("SELECT r FROM Record r WHERE r.metadata.status = :status ORDER BY r.metadata.modifiedDate", Record::class.java)
                .setParameter("status", RecordStatus.valueOf("READY"))
                .setMaxResults(limit)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .resultStream
                .peek {
                    it.metadata.status = RecordStatus.QUEUED
                    it.metadata.message = "Record is queued for processing"
                    it.metadata.revision += 1
                    it.metadata.modifiedDate = Instant.now()
                    merge(it.metadata)
                }
                .toList()
    }

    override fun readRecordContent(recordId: Long, fileName: String): RecordContent? = em.get().transact {
        val queryString = "SELECT rc from RecordContent rc WHERE rc.record.id = :id AND rc.fileName = :fileName"

        createQuery(queryString, RecordContent::class.java)
                .setParameter("fileName", fileName)
                .setParameter("id", recordId)
                .resultList.firstOrNull()
    }

    override fun readFileContent(id: Long, fileName: String): ByteArray? = em.get().transact {
        val queryString = "SELECT rc.content from RecordContent rc WHERE rc.record.id = :id AND rc.fileName = :fileName"

        createQuery(queryString, Blob::class.java)
                .setParameter("fileName", fileName)
                .setParameter("id", id)
                .resultList.firstOrNull()?.binaryStream?.readAllBytes()
    }

    override fun create(record: Record): Record = em.get().transact {
        val tmpContent = record.contents?.also { record.contents = null }

        persist(record)

        record.apply {
            contents = tmpContent?.mapTo(HashSet()) {
                it.record = record
                it.createdDate = Instant.now()
                persist(it)
                it
            }

            metadata = RecordMetadata().apply {
                this.record = record
                status = if (tmpContent == null) RecordStatus.INCOMPLETE else RecordStatus.READY
                message = if (tmpContent == null) "No data uploaded yet" else "Data successfully uploaded, ready for processing."
                createdDate = Instant.now()
                modifiedDate = Instant.now()
                revision = 1
                persist(this)
            }
        }
    }

    override fun read(id: Long): Record? = em.get().transact { find(Record::class.java, id) }

    override fun update(record: Record): Record = em.get().transact {
        record.metadata.update()
        merge<Record>(record)
    }

    private fun RecordMetadata.update() {
        this.revision += 1
        this.modifiedDate = Instant.now()
    }

    private fun modifyNow(metadata: RecordMetadata): RecordMetadata {
        metadata.update()
        return em.get().merge<RecordMetadata>(metadata)
    }

    override fun updateMetadata(id: Long, metadata: RecordMetadataDTO): RecordMetadata = em.get().transact {
        val existingMetadata = find(RecordMetadata::class.java, id,  LockModeType.PESSIMISTIC_WRITE,
                mapOf("javax.persistence.lock.scope" to PessimisticLockScope.EXTENDED))
                ?: throw NotFoundException("RecordMetadata with ID $id does not exist")

        if (existingMetadata.revision != metadata.revision)
            throw ConflictException("incompatible_revision", "Requested meta data revision ${metadata.revision} " +
                    "should match the latest existing revision ${existingMetadata.revision}")

        if (metadata.status == RecordStatus.PROCESSING.toString()
                && existingMetadata.status != RecordStatus.QUEUED) {
            throw ConflictException("incompatible_status", "Record cannot be updated: Conflict in record meta-data status. " +
                    "Found ${existingMetadata.status}, expected ${RecordStatus.QUEUED}")
        }

        if ((metadata.status == RecordStatus.SUCCEEDED.toString()
                || metadata.status == RecordStatus.FAILED.toString())
                && existingMetadata.status != RecordStatus.PROCESSING) {
            throw ConflictException("incompatible_status", "Record cannot be updated: Conflict in record meta-data status. " +
                    "Found ${existingMetadata.status}, expected ${RecordStatus.PROCESSING}")
        }
        logger.debug("Updating record $id status from ${existingMetadata.status} to ${metadata.status}")
        existingMetadata.apply {
            status = RecordStatus.valueOf(metadata.status)
            message = metadata.message
        }.update()

        merge<RecordMetadata>(existingMetadata)
    }

    override fun delete(record: Record) = em.get().transact { remove(record) }

    override fun readMetadata(id: Long): RecordMetadata? = em.get().transact { find(RecordMetadata::class.java, id) }

    companion object {
        private val logger = LoggerFactory.getLogger(RecordRepositoryImpl::class.java)
    }
}
