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

import org.hibernate.engine.jdbc.BlobProxy
import org.hibernate.engine.jdbc.ClobProxy
import org.radarbase.jersey.exception.HttpBadRequestException
import org.radarbase.jersey.exception.HttpConflictException
import org.radarbase.jersey.exception.HttpNotFoundException
import org.radarbase.jersey.hibernate.HibernateRepository
import org.radarbase.upload.api.ContentsDTO
import org.radarbase.upload.api.Page
import org.radarbase.upload.api.RecordMetadataDTO
import org.radarbase.upload.doa.entity.*
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.Reader
import java.sql.Blob
import java.time.Duration
import java.time.Instant
import java.util.*
import jakarta.inject.Provider
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import jakarta.persistence.PessimisticLockScope
import jakarta.ws.rs.core.Context
import kotlin.collections.HashSet


class RecordRepositoryImpl(
        @Context em: Provider<EntityManager>,
) : HibernateRepository(em), RecordRepository {
    override fun updateLogs(id: Long, logsData: String): RecordMetadata = transact {
        val logs = find(RecordLogs::class.java, id)
        val metadataToSave = if (logs == null) {
            val metadataFromDb = find(RecordMetadata::class.java, id)
                    ?: throw HttpNotFoundException("record_not_found", "RecordMetadata with ID $id does not exist")
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

        metadataToSave.update()
        merge(metadataToSave)
    }

    override fun query(page: Page, projectId: String, userId: String?, status: String?, sourceType: String?): Pair<List<Record>, Page> {
        var queryString = "SELECT r FROM Record r WHERE r.projectId = :projectId "
        var countQueryString = "SELECT count(r) FROM Record r WHERE r.projectId = :projectId "
        userId?.let {
            queryString += " AND r.userId = :userId"
            countQueryString += " AND r.userId = :userId"
        }
        status?.let {
            queryString += " AND r.metadata.status = :status"
            countQueryString += " AND r.metadata.status = :status"
        }
        sourceType?.let {
            queryString += " AND r.sourceType.name = :sourceType"
            countQueryString += " AND r.sourceType.name = :sourceType"
        }
        queryString += " ORDER BY r.id DESC"

        val actualPage = page.createValid(maximum = 100)

        return transact {
            val query = createQuery(queryString, Record::class.java)
                    .setParameter("projectId", projectId)
                    .setFirstResult(actualPage.offset)
                    .setMaxResults(actualPage.pageSize!!)

            val countQuery = createQuery(countQueryString)
                    .setParameter("projectId", projectId)

            userId?.let {
                query.setParameter("userId", it)
                countQuery.setParameter("userId", it)
            }
            status?.let {
                query.setParameter("status", RecordStatus.valueOf(it))
                countQuery.setParameter("status", RecordStatus.valueOf(it))
            }
            sourceType?.let {
                query.setParameter("sourceType", it)
                countQuery.setParameter("sourceType", it)
            }
            val records = query.resultList
            val count = countQuery.singleResult as Long

            Pair(records, actualPage.copy(totalElements = count))
        }
    }

    override fun readLogs(id: Long): RecordLogs? = transact {
        find(RecordLogs::class.java, id)
    }

    override fun readLogContents(id: Long): RecordRepository.ClobReader? = createTransaction { transaction ->
        val logs = find(RecordLogs::class.java, id)?.logs ?: return@createTransaction null

        object : RecordRepository.ClobReader {
            override val stream: Reader = logs.characterStream

            override fun close() {
                logs.free()
                transaction.close()
            }
        }
    }

    override fun updateContent(record: Record, fileName: String, contentType: String, stream: InputStream, length: Long): RecordContent = transact {
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
            record.contents?.add(it) ?: run {
                record.contents = mutableSetOf(it)
            }
        }

        merge(record)
        result
    }


    override fun deleteContents(record: Record, fileName: String): Unit = transact {
        refresh(record)
        if (record.metadata.status != RecordStatus.INCOMPLETE) {
            throw HttpConflictException("incompatible_status", "Cannot delete file contents from record ${record.id} that is already saved with status ${record.metadata.status}.")
        }
        val existingContent = record.contents?.find { it.fileName == fileName } ?: throw HttpNotFoundException("file_not_found", "Cannot file $fileName in record ${record.id}")
        record.contents?.remove(existingContent)
        remove(existingContent)
        merge(record)
    }

    override fun poll(limit: Int, supportedConverters: Set<String>): List<Record> = transact {
        setProperty("jakarta.persistence.lock.scope", PessimisticLockScope.EXTENDED)

        var queryString = "SELECT r FROM Record r WHERE r.metadata.status = :status "

        if (supportedConverters.isNotEmpty()) {
            queryString += " AND r.sourceType.name in :sourceTypes"
        }

        queryString += " ORDER BY r.metadata.modifiedDate"
        val query = createQuery(queryString, Record::class.java)
                .setParameter("status", RecordStatus.READY)
                .setMaxResults(limit)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)

        if (supportedConverters.isNotEmpty()) {
            query.setParameter("sourceTypes",supportedConverters)
        }

        query.resultStream
                .peek {
                    merge(it.metadata.apply {
                        record = it
                        status = RecordStatus.QUEUED
                        message = "Record is queued for processing"
                        revision += 1
                        modifiedDate = Instant.now()
                    })
                }
                .toList()
    }

    override fun readRecordContent(recordId: Long, fileName: String): RecordContent? = transact {
        val queryString = "SELECT rc from RecordContent rc WHERE rc.record.id = :id AND rc.fileName = :fileName"

        createQuery(queryString, RecordContent::class.java)
                .setParameter("fileName", fileName)
                .setParameter("id", recordId)
                .resultList.firstOrNull()
    }

    override fun readFileContent(
        id: Long,
        revision: Int,
        fileName: String,
        range: LongRange?,
    ): RecordRepository.BlobReader? = createTransaction { transaction ->
        val queryString = "SELECT rc.content from RecordContent rc WHERE rc.record.id = :id AND rc.fileName = :fileName"

        val blob = createQuery(queryString, Blob::class.java)
                .setParameter("fileName", fileName)
                .setParameter("id", id)
                .resultList.firstOrNull() ?: return@createTransaction null

        object : RecordRepository.BlobReader {
            override val stream: InputStream = if (range == null
                    || (range.first == 0L && range.count().toLong() == blob.length())) {
                blob.binaryStream
            } else {
                val offset = range.first + 1
                val limit = range.count().toLong()
                logger.debug("Reading record {} file {} from offset {} with length {}", id, fileName, offset, limit)
                blob.getBinaryStream(offset, limit)
            }

            override fun close() {
                blob.free()
                transaction.close()
            }
        }
    }

    override fun create(record: Record, metadata: RecordMetadata?, contents: Set<ContentsDTO>?): Record = transact {
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

    override fun read(id: Long): Record? = transact { find(Record::class.java, id) }

    override fun update(record: Record): Record = transact {
        record.metadata.update()
        merge(record)
    }

    private fun RecordMetadata.update() {
        this.revision += 1
        this.modifiedDate = Instant.now()
    }

    override fun updateMetadata(id: Long, metadata: RecordMetadataDTO): RecordMetadata = transact {
        val existingMetadata = find(RecordMetadata::class.java, id,  LockModeType.PESSIMISTIC_WRITE,
                mapOf("jakarta.persistence.lock.scope" to PessimisticLockScope.EXTENDED))
                ?: throw HttpNotFoundException("record_not_found", "RecordMetadata with ID $id does not exist")

        if (existingMetadata.revision != metadata.revision)
            throw HttpConflictException("incompatible_revision", "Requested meta data revision ${metadata.revision} " +
                    "should match the latest existing revision ${existingMetadata.revision}")

        logger.debug("Updating record $id status from ${existingMetadata.status} to ${metadata.status}")
        existingMetadata.apply {
            metadata.status?.let { newStatus ->
                if (!allowedStateTransition(existingMetadata.status, newStatus)) {
                    throw HttpConflictException("incompatible_status", "Record cannot be updated: Conflict in record meta-data status. " +
                            "Cannot transition from state ${existingMetadata.status} to ${metadata.status}")
                }

                status = RecordStatus.valueOf(newStatus)
            }
            message = metadata.message ?: status.defaultStatusMessage
        }.update()

        merge(existingMetadata)
    }

    override fun delete(record: Record, revision: Int) = transact {
        refresh(record)
        if (record.metadata.revision != revision) {
            throw HttpConflictException("incompatible_revision", "Revision in metadata ${record.metadata.revision} does not match provided revision $revision")
        }
        if (record.metadata.status == RecordStatus.PROCESSING) {
            throw HttpConflictException("incompatible_status", "Cannot delete record ${record.id}: it is currently being processed.")
        }
        remove(record)
    }

    override fun readMetadata(id: Long): RecordMetadata? = transact { find(RecordMetadata::class.java, id) }

    override fun resetStaleProcessing(sourceType: String, age: Duration): Int = transact {
        val now = Instant.now()
        createQuery("UPDATE RecordMetadata metadata SET metadata.status = :newStatus, metadata.revision = metadata.revision + 1, metadata.modifiedDate = :now WHERE metadata.status = :currentStatus AND metadata.modifiedDate < :staleDate")
                .setParameter("newStatus", RecordStatus.READY)
                .setParameter("currentStatus", RecordStatus.PROCESSING)
                .setParameter("now", now)
                .setParameter("staleDate", now.minus(age))
                .executeUpdate()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RecordRepositoryImpl::class.java)

        private fun allowedStateTransition(from: RecordStatus, to: String): Boolean {
            val toStatus = try {
                RecordStatus.valueOf(to)
            } catch (ex: IllegalArgumentException) {
                throw HttpBadRequestException("unknown_status", "Record status $to is not a known status. Use one of: ${RecordStatus.values().joinToString()}.")
            }

            return if (toStatus == from) {
                true
            } else {
                val allowedStateTransition = allowedStateTransitions[from]
                    ?: throw HttpBadRequestException(
                        "unknown_status",
                        "Record status $from is not a known status. Use one of: ${allowedStateTransitions.keys}.",
                    )

                toStatus in allowedStateTransition
            }
        }

        private val allowedStateTransitions: Map<RecordStatus, Set<RecordStatus>> = listOf(
            RecordStatus.INCOMPLETE to EnumSet.of(RecordStatus.READY, RecordStatus.FAILED),
            RecordStatus.READY to EnumSet.of(RecordStatus.QUEUED, RecordStatus.FAILED),
            RecordStatus.QUEUED to EnumSet.of(RecordStatus.PROCESSING, RecordStatus.READY, RecordStatus.FAILED),
            RecordStatus.PROCESSING to EnumSet.of(RecordStatus.READY, RecordStatus.SUCCEEDED, RecordStatus.FAILED),
            RecordStatus.FAILED to EnumSet.of(RecordStatus.READY),
            RecordStatus.SUCCEEDED to EnumSet.of(RecordStatus.READY),
        ).toMap(EnumMap(RecordStatus::class.java))

        private val RecordStatus.defaultStatusMessage: String
            get() = when(this) {
                RecordStatus.INCOMPLETE -> "The record has been created. The data must be uploaded"
                RecordStatus.READY -> "The record is ready for processing"
                RecordStatus.QUEUED -> "The record has been queued for processing"
                RecordStatus.PROCESSING -> "The record is being processed"
                RecordStatus.FAILED -> "The record processing has been failed. Please check the logs for more information"
                RecordStatus.SUCCEEDED -> "The record has been processed successfully"
            }
    }
}
