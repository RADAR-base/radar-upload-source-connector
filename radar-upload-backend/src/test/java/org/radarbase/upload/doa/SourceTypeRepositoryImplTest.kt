package org.radarbase.upload.doa

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.radarbase.upload.Config
import org.radarbase.upload.doa.entity.SourceType
import org.radarbase.upload.inject.DoaEntityManagerFactory
import java.nio.file.Path
import javax.persistence.EntityManager

internal class SourceTypeRepositoryImplTest {
    private lateinit var repository: SourceTypeRepository
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
        repository = SourceTypeRepositoryImpl(entityManager)
    }

    @AfterEach
    fun tearDown() {
        doaFactory.dispose(entityManager)
    }

    @Test
    fun readList() {
        val sourceType = SourceType().apply {
            name = "Mp3Audio"
            topics = setOf("topic1", "topic2")
            contentTypes = setOf("application/mp3", "audio/mp3")
            configuration = mapOf(
                    Pair("a", "c"),
                    Pair("b", "d")
            )
        }

        val anotherSourceType = SourceType().apply {
            name = "TextFile"
            topics = setOf("topic3", "topic4")
            contentTypes = setOf("application/text")
            configuration = mapOf(
                    Pair("a", "c"),
                    Pair("b", "d")
            )
        }
        repository.create(sourceType)
        repository.create(anotherSourceType)
        entityManager.clear()
        val result = repository.read(10, null)
        assertThat(result, notNullValue())
        assertThat(result.size, equalTo(2))

    }


    @Test
    fun read() {
        val sourceType = SourceType().apply {
            name = "Mp3Audio"
            topics = setOf("topic1", "topic2")
            contentTypes = setOf("application/mp3", "audio/mp3")
            configuration = mapOf(
                    Pair("a", "c"),
                    Pair("b", "d")
            )
        }
        repository.create(sourceType)
        entityManager.clear()
        val result = repository.read(1,"Mp3Audio").firstOrNull()
        assertThat(result, notNullValue())
        assertThat(result?.name, equalTo("Mp3Audio"))
        assertThat(result?.topics, equalTo(setOf("topic1", "topic2")))
        assertThat(entityManager.entityManagerFactory.persistenceUnitUtil.isLoaded(result?.configuration), `is`(false))
    }

    @Test
    fun readDetailed() {
        val sourceType = SourceType().apply {
            name = "Mp3Audio"
            topics = setOf("topic1", "topic2")
            contentTypes = setOf("application/mp3", "audio/mp3")
            configuration = mapOf(
                    Pair("a", "c"),
                    Pair("b", "d")
            )
        }
        repository.create(sourceType)
        entityManager.clear()
        val result = repository.read("Mp3Audio")
        assertThat(result, notNullValue())
        assertThat(result?.name, equalTo("Mp3Audio"))
        assertThat(result?.topics, equalTo(setOf("topic1", "topic2")))
        assertThat(entityManager.entityManagerFactory.persistenceUnitUtil.isLoaded(result?.configuration), `is`(true))
    }

    @Test
    fun delete() {
        val sourceType = SourceType().apply {
            name = "Mp3Audio"
            topics = setOf("topic1", "topic2")
            contentTypes = setOf("application/mp3", "audio/mp3")
            configuration = mapOf(
                    Pair("a", "c"),
                    Pair("b", "d")
            )
        }
        assertThat(repository.read("Mp3Audio"), nullValue())
        repository.create(sourceType)
        assertThat(repository.read("Mp3Audio"), notNullValue())
        repository.delete(sourceType)
        assertThat(repository.read("Mp3Audio"), nullValue())
    }
}
