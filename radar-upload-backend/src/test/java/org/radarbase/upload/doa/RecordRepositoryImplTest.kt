package org.radarbase.upload.doa

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.hibernate.engine.jdbc.BlobProxy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.radarbase.upload.Config
import org.radarbase.upload.doa.entity.Record
import org.radarbase.upload.doa.entity.RecordContent
import org.radarbase.upload.doa.entity.RecordStatus
import org.radarbase.upload.inject.DoaEntityManagerFactory
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDateTime
import javax.persistence.EntityManager
import kotlin.text.Charsets.UTF_8

internal class RecordRepositoryImplTest {
    private lateinit var repository: RecordRepository
    private lateinit var doaFactory: DoaEntityManagerFactory

    private lateinit var entityManager: EntityManager

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        doaFactory = DoaEntityManagerFactory(
                Config(jdbcUrl = "jdbc:h2:file:${tempDir.resolve("db.h2")};DB_CLOSE_DELAY=-1;MVCC=true"),
                mapOf(Pair("hibernate.show_sql", "true"))
        )
        entityManager = doaFactory.get()
        repository = RecordRepositoryImpl(entityManager)
    }

    @AfterEach
    fun tearDown() {
        doaFactory.dispose(entityManager)
    }

    @Test
    fun create() {
        doCreate()
    }

    private fun doCreate(): Record {
        val record = Record().apply {
            projectId = "p"
            userId = "u"
            sourceId = "s"
            time = LocalDateTime.parse("2019-01-01T00:00:00")
            timeZoneOffset = 3600

            contents = mutableSetOf(RecordContent().apply {
                fileName = "Gibson.mp3"
                contentType = "audio/mp3"
                content = BlobProxy.generateProxy("test".toByteArray())
            })
        }
        val beforeTime = Instant.now()
        val result = repository.create(record)
        val afterTime = Instant.now()

        assertThat(result.id, notNullValue())
        assertThat(result.id ?: 0L, greaterThan(0L))
        assertThat(result.contents, notNullValue())
        result.contents?.first()?.let {
            assertThat(it.content, notNullValue())
            assertThat(it.content.binaryStream?.readAllBytes()?.toString(UTF_8), equalTo("test"))
        }
        assertThat(result.metadata, notNullValue())
        result.metadata.let {
            assertThat(it.createdDate, greaterThanOrEqualTo(beforeTime))
            assertThat(it.createdDate, lessThanOrEqualTo(afterTime))
            assertThat(it.modifiedDate, greaterThanOrEqualTo(beforeTime))
            assertThat(it.modifiedDate, lessThanOrEqualTo(afterTime))
            assertThat(it.committedDate, nullValue())
            assertThat(it.status, sameInstance(RecordStatus.READY))
        }
        return result
    }

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

            contents = mutableSetOf(RecordContent().apply {
                fileName = "Gibson.mp3"
                contentType = "audio/mp3"
                content = BlobProxy.generateProxy("test".toByteArray())
            })
        }

        val result = repository.create(record)

        assertThat(result.contents, hasSize(1))

        val newContent = "test2".toByteArray()
        repository.updateContent(result, "Gibson2.mp4", "audio/mp4",
                ByteArrayInputStream(newContent), newContent.size.toLong())

        assertThat(result.contents, hasSize(2))

        assertThat(result.contents, notNullValue())
        result.contents?.find { it.fileName == "Gibson2.mp4" }?.let {
            assertThat(it.content, notNullValue())
            assertThat(it.content.binaryStream?.readAllBytes()?.toString(UTF_8), equalTo("test2"))
            assertThat(it.fileName, equalTo("Gibson2.mp4"))
            assertThat(it.contentType, equalTo("audio/mp4"))
        }

        result.contents?.find { it.fileName == "Gibson.mp3" }?.let {
            assertThat(it.content, notNullValue())
            assertThat(it.content.binaryStream?.readAllBytes()?.toString(UTF_8), equalTo("test"))
            assertThat(it.fileName, equalTo("Gibson.mp3"))
            assertThat(it.contentType, equalTo("audio/mp3"))
        } ?: assert(false)

        repository.updateContent(result, "Gibson.mp3", "audio/mp4",
                ByteArrayInputStream(newContent), newContent.size.toLong())

        assertThat(result.contents, hasSize(2))

        result.contents?.find { it.fileName == "Gibson.mp3" }?.let {
            assertThat(it.content, notNullValue())
            assertThat(it.content.binaryStream?.readAllBytes()?.toString(UTF_8), equalTo("test2"))
            assertThat(it.fileName, equalTo("Gibson.mp3"))
            assertThat(it.contentType, equalTo("audio/mp4"))
        } ?: assert(false)
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
        result.contents?.find { it.fileName == "Gibson2.mp4" }?.let {
            assertThat(it.content, notNullValue())
            assertThat(it.content.binaryStream?.readAllBytes()?.toString(UTF_8), equalTo("test2"))
            assertThat(it.fileName, equalTo("Gibson2.mp4"))
            assertThat(it.contentType, equalTo("audio/mp4"))
        } ?: assert(false)
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
        repository.updateLogs(record.id!!, StringReader(log), log.length.toLong())
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
        repository.delete(record)
        assertThat(repository.read(id), nullValue())
        assertThat(repository.readContent(id, "Gibson.mp3"), nullValue())
        assertThat(repository.readLogs(id), nullValue())
        assertThat(repository.readMetadata(id), nullValue())
    }

    @Test
    fun close() {
    }
}
