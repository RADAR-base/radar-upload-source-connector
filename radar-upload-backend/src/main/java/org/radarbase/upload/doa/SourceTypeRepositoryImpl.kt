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

import jakarta.inject.Provider
import jakarta.persistence.EntityManager
import jakarta.ws.rs.core.Context
import org.radarbase.jersey.hibernate.HibernateRepository
import org.radarbase.jersey.service.AsyncCoroutineService
import org.radarbase.upload.api.SourceTypeDTO
import org.radarbase.upload.api.SourceTypeMapper
import org.radarbase.upload.doa.entity.SourceType
import org.slf4j.LoggerFactory

class SourceTypeRepositoryImpl(
    @Context em: Provider<EntityManager>,
    @Context asyncService: AsyncCoroutineService,
    @Context private val sourceTypeMapper: SourceTypeMapper,
) : HibernateRepository(em, asyncService), SourceTypeRepository {

    suspend fun storeSourceTypesFromConfigs(sourceTypeDtos: List<SourceTypeDTO>) {
        val sourceTypes = sourceTypeDtos.map { sourceTypeMapper.toSourceType(it) }

        transact {
            sourceTypes.forEach { sourceType ->
                logger.info("Initializing source-type repository")
                val queryString = "SELECT s FROM SourceType s LEFT JOIN FETCH s.configuration WHERE s.name = :name"

                val result = createQuery(queryString, SourceType::class.java).run {
                    setParameter("name", sourceType.name)
                    resultList.firstOrNull()
                }
                if (result != null) {
                    logger.info("Updating source-type with name ${sourceType.name}")
                    merge(result.copy(sourceType))
                } else {
                    logger.info("Creating source-type with name ${sourceType.name}")
                    persist(sourceType)
                }
            }
        }
    }

    fun SourceType.copy(sourceType: SourceType): SourceType {
        this.topics = sourceType.topics
        this.timeRequired = sourceType.timeRequired
        this.sourceIdRequired = sourceType.sourceIdRequired
        this.contentTypes = sourceType.contentTypes
        this.configuration = sourceType.configuration
        return this
    }

    override suspend fun create(record: SourceType) = transact { persist(record) }

    override suspend fun readAll(limit: Int?, lastId: Long?, detailed: Boolean): List<SourceType> {
        var queryString = "SELECT s FROM SourceType s"
        lastId?.let {
            queryString += " WHERE s.id > :lastId "
        }
        if (detailed) {
            queryString += " JOIN FETCH s.configuration"
        }
        queryString += " ORDER BY s.id"

        return transact {
            createQuery(queryString, SourceType::class.java).run {
                limit?.let {
                    maxResults = it
                }
                lastId?.let {
                    setParameter("lastId", it)
                }
                logger.info("QUERY PARAMETERS: {}", parameters)
                resultList
            }
        }
    }

    override suspend fun read(name: String): SourceType? = transact {
        val queryString = "SELECT s FROM SourceType s LEFT JOIN FETCH s.configuration WHERE s.name = :name"

        createQuery(queryString, SourceType::class.java).run {
            setParameter("name", name)
            logger.debug("Query source type with name $name: {}", parameters.map { it.name })
            resultList.firstOrNull()
        }
    }

    override suspend fun update(record: SourceType): SourceType = transact { merge(record) }

    override suspend fun delete(record: SourceType) = transact { remove(record) }

    companion object {
        private val logger = LoggerFactory.getLogger(SourceTypeRepositoryImpl::class.java)
    }
}
