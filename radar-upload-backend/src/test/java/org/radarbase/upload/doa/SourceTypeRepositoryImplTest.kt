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
import org.radarbase.upload.api.SourceTypeMapper
import org.radarbase.upload.api.SourceTypeMapperImpl
import org.radarbase.upload.doa.entity.SourceType
import org.radarbase.upload.inject.DoaEntityManagerFactoryFactory
import java.nio.file.Path
import javax.inject.Provider
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory

internal class SourceTypeRepositoryImplTest {
    private lateinit var repository: SourceTypeRepository
    private lateinit var doaEMFFactory: DoaEntityManagerFactoryFactory
    private lateinit var doaEMF: EntityManagerFactory
    private lateinit var sourceTypeMapper: SourceTypeMapper
    private lateinit var entityManager: EntityManager

    @TempDir
    lateinit var tempDir: Path

    @Mock
    lateinit var mockEntityManagerProvider: Provider<EntityManager>

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        val config = Config(
                jdbcUrl = "jdbc:h2:file:${tempDir.resolve("db.h2")};DB_CLOSE_DELAY=-1",
                hibernateDialect = "org.hibernate.dialect.H2Dialect")
        doaEMFFactory = DoaEntityManagerFactoryFactory(config)
        doaEMF = doaEMFFactory.get()
        entityManager = doaEMF.createEntityManager()
        sourceTypeMapper = SourceTypeMapperImpl()
        repository = SourceTypeRepositoryImpl(mockEntityManagerProvider, config, sourceTypeMapper)
        Mockito.`when`(mockEntityManagerProvider.get()).thenReturn(entityManager)
    }

    @AfterEach
    fun tearDown() {
        doaEMFFactory.dispose(doaEMF)
        entityManager.close()
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
