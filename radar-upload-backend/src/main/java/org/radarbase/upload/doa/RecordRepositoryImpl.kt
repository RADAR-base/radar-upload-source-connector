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
import org.hibernate.engine.jdbc.BlobProxy
import org.hibernate.engine.jdbc.ClobProxy
import org.radarbase.upload.api.ContentsDTO
import org.radarbase.upload.api.RecordMetadataDTO
import org.radarbase.upload.doa.entity.*
import org.radarbase.upload.exception.BadRequestException
import org.radarbase.upload.exception.ConflictException
import org.radarbase.upload.exception.NotFoundException
import org.radarbase.upload.inject.session
import org.radarbase.upload.inject.transact
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.sql.Blob
import java.time.Instant
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.LockModeType
import javax.persistence.PessimisticLockScope
import javax.ws.rs.core.Context
import kotlin.collections.HashSet
import kotlin.streams.toList


class RecordRepositoryImpl(@Context private var em: javax.inject.Provider<EntityManager>) : RecordRepository {

    override fun updateLogs(id: Long, logsData: String): RecordMetadata = em.get().transact {
        val logs = find(RecordLogs::class.java, id)
        val metadataToSave = if (logs == null) {
            val metadataFromDb = find(RecordMetadata::class.java, id)
                    ?: throw NotFoundException("record_not_found", "RecordMetadata with ID $id does not exist")
            refresh(metadataFromDb)
            metadataFromDb.logs = RecordLogs().apply {
                this.metadata = metadataFromDb
                this.modifiedDate = Instant.now()
                this.size = logsData.length.toLong()
                this.logs = ClobProxy.generateProxy(logsData)
            }.also {
                persist(it)
            }
            metadataFromDb
        } else {
            logs.apply {
                this.modifiedDate = Instant.now()
                this.logs = ClobProxy.generateProxy(logsData)
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

    override fun readLogContents(id: Long): String? = em.get().transact {
        find(RecordLogs::class.java, id)?.logs?.characterStream?.readText()
    }

    override fun updateContent(record: Record, fileName: String, contentType: String, stream: InputStream, length: Long): RecordContent = em.get().transact {
        val existingContent = record.contents?.find { it.fileName == fileName }

        val result = existingContent?.apply {
            this.createdDate = Instant.now()
            this.contentType = contentType
            this.content = BlobProxy.generateProxy(stream, length)
        } ?: RecordContent().apply {
            this.record = record
            this.fileName = fileName
            this.createdDate = Instant.now()
            this.contentType = contentType
            this.size = length
            this.content = BlobProxy.generateProxy(stream, length)
        }.also {
            if (record.contents != null) {
                record.contents?.add(it)
            } else {
                record.contents = mutableSetOf(it)
            }
        }

        merge(record)
        result
    }


    override fun deleteContents(record: Record, fileName: String): Unit = em.get().transact {
        refresh(record)
        if (record.metadata.status != RecordStatus.INCOMPLETE) {
            throw ConflictException("incompatible_status", "Cannot delete file contents from record ${record.id} that is already saved with status ${record.metadata.status}.")
        }
        val existingContent = record.contents?.find { it.fileName == fileName } ?: throw NotFoundException("file_not_found", "Cannot file $fileName in record ${record.id}")
        record.contents?.remove(existingContent)
        remove(existingContent)
        merge(record)
    }

    override fun poll(limit: Int): List<Record> = em.get().transact {
        setProperty("javax.persistence.lock.scope", PessimisticLockScope.EXTENDED)
        createQuery("SELECT r FROM Record r WHERE r.metadata.status = :status ORDER BY r.metadata.modifiedDate", Record::class.java)
                .setParameter("status", RecordStatus.valueOf("READY"))
                .setMaxResults(limit)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .resultStream
                .peek {
                    it.metadata.apply {
                        status = RecordStatus.QUEUED
                        message = "Record is queued for processing"
                        revision += 1
                        modifiedDate = Instant.now()
                        merge(this)
                    }
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

    override fun readFileContent(id: Long, revision: Int, fileName: String, offset: Long, limit: Long): ByteArray? = em.get().transact {
        logger.info("Reading record $id file $fileName from offset $offset with length $limit")
        val queryString = "SELECT rc.content from RecordContent rc WHERE rc.record.id = :id AND rc.fileName = :fileName"

        val blob = createQuery(queryString, Blob::class.java)
                .setParameter("fileName", fileName)
                .setParameter("id", id)
                .resultList.firstOrNull() ?: return@transact null

        val result = blob.getBinaryStream(offset + 1, limit)?.use {
            it.readBytes()
        }
        blob.free()
        result
    }

    override fun create(record: Record, metadata: RecordMetadata?, contents: Set<ContentsDTO>?): Record = em.get().transact {
        persist(record)

        record.contents = contents
                ?.filter { it.text != null }
                ?.takeIf { it.isNotEmpty() }
                ?.mapTo(HashSet()) { content ->
                    RecordContent().apply {
                        this.record = record
                        this.fileName = content.fileName
                        this.createdDate = Instant.now()
                        this.contentType = content.contentType
                        this.size = content.text!!.length.toLong()
                        this.content = BlobProxy.generateProxy(content.text!!.toByteArray(Charsets.UTF_8))
                    }
                }
                ?.onEach { persist(it) }

        record.metadata = RecordMetadata().apply {
            this.record = record
            if (metadata?.status == RecordStatus.READY && record.contents != null) {
                status = RecordStatus.READY
                message = metadata.message ?: "Data had been uploaded and ready for processing"
            } else {
                status = RecordStatus.INCOMPLETE
                message = metadata?.message ?: "Record has been created. Data must be uploaded"
            }
            createdDate = Instant.now()
            modifiedDate = Instant.now()
            revision = 1
            callbackUrl = metadata?.callbackUrl
            persist(this)
        }

        record
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
                ?: throw NotFoundException("record_not_found", "RecordMetadata with ID $id does not exist")

        if (existingMetadata.revision != metadata.revision)
            throw ConflictException("incompatible_revision", "Requested meta data revision ${metadata.revision} " +
                    "should match the latest existing revision ${existingMetadata.revision}")

        logger.debug("Updating record $id status from ${existingMetadata.status} to ${metadata.status}")
        existingMetadata.apply {
            metadata.status?.let { newStatus ->
                if (!allowedStateTransition(existingMetadata.status, newStatus)) {
                    throw ConflictException("incompatible_status", "Record cannot be updated: Conflict in record meta-data status. " +
                            "Cannot transition from state ${existingMetadata.status} to ${metadata.status}")
                }

                status = RecordStatus.valueOf(newStatus)
            }
            message = metadata.message
        }.update()

        merge<RecordMetadata>(existingMetadata)
    }

    override fun delete(record: Record, revision: Int) = em.get().transact {
        refresh(record)
        if (record.metadata.revision != revision) {
            throw ConflictException("incompatible_revision", "Revision in metadata ${record.metadata.revision} does not match provided revision $revision")
        }
        if (record.metadata.status == RecordStatus.PROCESSING) {
            throw ConflictException("incompatible_status", "Cannot delete record ${record.id}: it is currently being processed.")
        }
        remove(record)
    }

    override fun readMetadata(id: Long): RecordMetadata? = em.get().transact { find(RecordMetadata::class.java, id) }

    companion object {
        private val logger = LoggerFactory.getLogger(RecordRepositoryImpl::class.java)

        private fun allowedStateTransition(from: RecordStatus, to: String): Boolean {
            val toStatus = try {
                RecordStatus.valueOf(to)
            } catch (ex: IllegalArgumentException) {
                throw BadRequestException("unknown_status", "Record status $to is not a known status. Use one of: ${RecordStatus.values().joinToString()}.")
            }

            return toStatus == from
                    || toStatus in allowedStateTransitions[from]
                    ?: throw BadRequestException("unknown_status", "Record status $from is not a known status. Use one of: ${RecordStatus.values().joinToString()}.")
        }

        private val allowedStateTransitions: Map<RecordStatus, Set<RecordStatus>> = EnumMap<RecordStatus, Set<RecordStatus>>(RecordStatus::class.java).apply {
            this[RecordStatus.INCOMPLETE] = setOf(RecordStatus.READY, RecordStatus.FAILED)
            this[RecordStatus.READY] = setOf(RecordStatus.QUEUED, RecordStatus.FAILED)
            this[RecordStatus.QUEUED] = setOf(RecordStatus.PROCESSING, RecordStatus.READY, RecordStatus.FAILED)
            this[RecordStatus.PROCESSING] = setOf(RecordStatus.READY, RecordStatus.SUCCEEDED, RecordStatus.FAILED)
            this[RecordStatus.FAILED] = setOf()
            this[RecordStatus.SUCCEEDED] = setOf()
        }

    }
}
