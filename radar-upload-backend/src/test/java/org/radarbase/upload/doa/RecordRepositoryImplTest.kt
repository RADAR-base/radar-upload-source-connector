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

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.radarbase.upload.Config
import org.radarbase.upload.api.ContentsDTO
import org.radarbase.upload.api.RecordMetadataDTO
import org.radarbase.upload.doa.entity.Record
import org.radarbase.upload.doa.entity.RecordStatus
import org.radarbase.upload.inject.DoaEntityManagerFactoryFactory
import java.io.ByteArrayInputStream
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDateTime
import javax.inject.Provider
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import kotlin.text.Charsets.UTF_8

internal class RecordRepositoryImplTest {
    private lateinit var repository: RecordRepository
    private lateinit var doaEMFFactory: DoaEntityManagerFactoryFactory
    private lateinit var doaEMF: EntityManagerFactory

    private lateinit var entityManager: EntityManager

    @TempDir
    lateinit var tempDir: Path

    @Mock
    lateinit var mockEntityManagerProvider: Provider<EntityManager>

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        doaEMFFactory = DoaEntityManagerFactoryFactory(
                Config(
                        jdbcUrl = "jdbc:h2:file:${tempDir.resolve("db.h2")};DB_CLOSE_DELAY=-1",
                        hibernateDialect = "org.hibernate.dialect.H2Dialect")
        )
        doaEMF = doaEMFFactory.get()
        entityManager = doaEMF.createEntityManager()
        repository = RecordRepositoryImpl(mockEntityManagerProvider)
        Mockito.`when`(mockEntityManagerProvider.get()).thenReturn(entityManager)
    }

    @AfterEach
    fun tearDown() {
        doaEMFFactory.dispose(doaEMF)
        entityManager.close()
    }

    @Test
    fun create() {
        val beforeTime = Instant.now()
        val result = doCreate()
        val afterTime = Instant.now()

        assertThat(result.id, notNullValue())
        assertThat(result.id ?: 0L, greaterThan(0L))
        assertThat(result.contents, notNullValue())
        val contents = repository.readFileContent(result.id!!, result.metadata.revision, "Gibson.mp3")
        assertThat(contents, notNullValue())
        assertThat(contents?.asString(), equalTo("test"))
        assertThat(result.metadata, notNullValue())
        result.metadata.run {
            assertThat(createdDate, greaterThanOrEqualTo(beforeTime))
            assertThat(createdDate, lessThanOrEqualTo(afterTime))
            assertThat(modifiedDate, greaterThanOrEqualTo(beforeTime))
            assertThat(modifiedDate, lessThanOrEqualTo(afterTime))
            assertThat(committedDate, nullValue())
            assertThat(status, sameInstance(RecordStatus.INCOMPLETE))
        }

        val metadata = repository.updateMetadata(result.id!!, RecordMetadataDTO(revision = 1, status = "READY"))

        assertThat(metadata.status, sameInstance(RecordStatus.READY))
        assertThat(metadata.revision, equalTo(2))
    }

    private fun doCreate() = repository.create(Record().apply {
            projectId = "p"
            userId = "u"
            sourceId = "s"
            time = LocalDateTime.parse("2019-01-01T00:00:00")
            timeZoneOffset = 3600
        }, contents = setOf(ContentsDTO(
            fileName = "Gibson.mp3",
            contentType = "audio/mp3",
            text = "test")))

    @Test
    fun readContent() {
    }

    @Test
    fun updateContent() {
        val record = Record().apply {
            projectId = "p"
            userId = "u"
            sourceId = "s"
            time = LocalDateTime.parse("2019-01-01T00:00:00")
            timeZoneOffset = 3600
        }

        val result = repository.create(record, contents = setOf(ContentsDTO(
            fileName = "Gibson.mp3",
            contentType = "audio/mp3",
            text = "test")))

        assertThat(result.contents, hasSize(1))

        val newContent = "test2".toByteArray()
        repository.updateContent(result, "Gibson2.mp4", "audio/mp4",
                ByteArrayInputStream(newContent), newContent.size.toLong())

        assertThat(result.contents, hasSize(2))

        assertThat(result.contents, notNullValue())

        result.contents?.find { it.fileName == "Gibson2.mp4" }?.let {
            assertThat(it.content, notNullValue())
            assertThat(repository.readFileContent(record.id!!, record.metadata.revision, it.fileName)?.asString(), equalTo("test2"))
            assertThat(it.fileName, equalTo("Gibson2.mp4"))
            assertThat(it.contentType, equalTo("audio/mp4"))
        }

        result.contents?.find { it.fileName == "Gibson.mp3" }?.let {
            assertThat(it.content, notNullValue())
            assertThat(repository.readFileContent(record.id!!, record.metadata.revision, it.fileName)?.asString(), equalTo("test"))
            assertThat(it.fileName, equalTo("Gibson.mp3"))
            assertThat(it.contentType, equalTo("audio/mp3"))
        } ?: assert(false)

        repository.updateContent(result, "Gibson.mp3", "audio/mp4",
                ByteArrayInputStream(newContent), newContent.size.toLong())

        assertThat(result.contents, hasSize(2))

        result.contents?.find { it.fileName == "Gibson.mp3" }?.let {
            assertThat(it.content, notNullValue())
            assertThat(repository.readFileContent(record.id!!, record.metadata.revision, it.fileName)?.asString(), equalTo("test2"))
            assertThat(it.fileName, equalTo("Gibson.mp3"))
            assertThat(it.contentType, equalTo("audio/mp4"))
        } ?: assert(false)
    }

    @Test
    fun deleteContent() {
        val record = Record().apply {
            projectId = "p"
            userId = "u"
            sourceId = "s"
            time = LocalDateTime.parse("2019-01-01T00:00:00")
            timeZoneOffset = 3600
        }

        val result = repository.create(record, contents = setOf(ContentsDTO(
                    fileName = "Gibson.mp3",
                    contentType = "audio/mp3",
                    text = "test")))

        assertThat(result.contents, hasSize(1))

        repository.deleteContents(result, "Gibson.mp3")
        assertThat(result.contents, hasSize(0))

        val newContent = "test2".toByteArray()
        repository.updateContent(result, "Gibson2.mp4", "audio/mp4",
                ByteArrayInputStream(newContent), newContent.size.toLong())

        assertThat(result.contents, hasSize(1))

        assertThat(result.contents, notNullValue())
        result.contents?.find { it.fileName == "Gibson2.mp4" }?.let {
            assertThat(it.content, notNullValue())
            assertThat(repository.readFileContent(record.id!!, record.metadata.revision, it.fileName)?.asString(), equalTo("test2"))
            assertThat(it.fileName, equalTo("Gibson2.mp4"))
            assertThat(it.contentType, equalTo("audio/mp4"))
        }
    }

    @Test
    fun createContent() {
        val record = Record().apply {
            projectId = "p"
            userId = "u"
            sourceId = "s"
            time = LocalDateTime.parse("2019-01-01T00:00:00")
            timeZoneOffset = 3600
        }

        val result = repository.create(record)

        val newContent = "test2".toByteArray()
        repository.updateContent(result, "Gibson2.mp4", "audio/mp4",
                ByteArrayInputStream(newContent), newContent.size.toLong())

        assertThat(result.contents, notNullValue())
        assertThat(result.contents, hasSize(1))
        assertThat(repository.readFileContent(result.id!!, 1, "Gibson2.mp4")?.asString(), equalTo("test2"))
    }

    @Test
    fun readLogs() {
        val record = Record().apply {
            projectId = "p"
            userId = "u"
            sourceId = "s"
            time = LocalDateTime.parse("2019-01-01T00:00:00")
            timeZoneOffset = 3600
        }

        val log = "test\ntest2"

        val beforeTime = Instant.now()
        repository.create(record)
        repository.updateLogs(record.id!!, log)
        val afterTime = Instant.now()

        val recordLogs = repository.readLogs(record.id!!)
        assertThat(recordLogs, notNullValue())
        assertThat(recordLogs?.logs, notNullValue())
        recordLogs?.logs?.let {
            assertThat(it.length(), equalTo(log.length.toLong()))
            assertThat(it.characterStream.readText(), equalTo("test\ntest2"))
        }
        assertThat(recordLogs?.modifiedDate ?: Instant.MIN, greaterThanOrEqualTo(beforeTime))
        assertThat(recordLogs?.modifiedDate ?: Instant.MAX, lessThanOrEqualTo(afterTime))
    }

    @Test
    fun read() {
    }

    @Test
    fun delete() {
        val record = doCreate()
        val id = record.id!!
        readLogs()
        repository.delete(record, 1)
        assertThat(repository.read(id), nullValue())
        assertThat(repository.readRecordContent(id, "Gibson.mp3"), nullValue())
        assertThat(repository.readLogs(id), nullValue())
        assertThat(repository.readMetadata(id), nullValue())
    }

    @Test
    fun close() {
    }

    private fun RecordRepository.BlobReader.asString(): String = use { it.stream.readBytes().toString(UTF_8) }
}
