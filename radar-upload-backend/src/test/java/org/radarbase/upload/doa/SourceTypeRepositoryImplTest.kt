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

import org.glassfish.jersey.server.monitoring.ApplicationEvent
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.radarbase.jersey.hibernate.DatabaseInitialization
import org.radarbase.jersey.hibernate.RadarEntityManagerFactoryFactory
import org.radarbase.jersey.hibernate.config.DatabaseConfig
import org.radarbase.upload.Config
import org.radarbase.upload.api.SourceTypeMapper
import org.radarbase.upload.api.SourceTypeMapperImpl
import org.radarbase.upload.doa.entity.*
import java.nio.file.Path
import jakarta.inject.Provider
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import kotlin.reflect.jvm.jvmName

internal class SourceTypeRepositoryImplTest {
    private lateinit var repository: SourceTypeRepository
    private lateinit var doaEMFFactory: RadarEntityManagerFactoryFactory
    private lateinit var doaEMF: EntityManagerFactory
    private lateinit var sourceTypeMapper: SourceTypeMapper
    private lateinit var entityManager: EntityManager

    private lateinit var closeable: AutoCloseable

    @Mock
    lateinit var mockEntityManagerProvider: Provider<EntityManager>

    @BeforeEach
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)

        val config = DatabaseConfig(
            managedClasses = listOf(
                Record::class.jvmName,
                RecordMetadata::class.jvmName,
                RecordLogs::class.jvmName,
                RecordContent::class.jvmName,
                SourceType::class.jvmName,
            ),
            url = "jdbc:hsqldb:mem:test1;DB_CLOSE_DELAY=-1",
            dialect = "org.hibernate.dialect.HSQLDialect",
        )
        doaEMFFactory = RadarEntityManagerFactoryFactory(config)
        doaEMF = doaEMFFactory.get()
        val eventStart = mock<ApplicationEvent> {
            on(ApplicationEvent::getType) doReturn ApplicationEvent.Type.INITIALIZATION_APP_FINISHED
        }
        DatabaseInitialization({ doaEMF }, config).onEvent(eventStart)
        entityManager = doaEMF.createEntityManager()
        sourceTypeMapper = SourceTypeMapperImpl()
        repository = SourceTypeRepositoryImpl(mockEntityManagerProvider, Config(), sourceTypeMapper)
        Mockito.`when`(mockEntityManagerProvider.get()).thenReturn(entityManager)
    }

    @AfterEach
    fun tearDown() {
        repository.readAll().forEach { repository.delete(it) }
        doaEMFFactory.dispose(doaEMF)
        entityManager.close()
        closeable.close()
    }

    @Test
    fun readAll() {
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
        val result = repository.readAll()
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
