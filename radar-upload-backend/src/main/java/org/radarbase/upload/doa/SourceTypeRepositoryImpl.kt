package org.radarbase.upload.doa

import org.radarbase.upload.Config
import org.radarbase.upload.api.SourceTypeDTO
import org.radarbase.upload.api.SourceTypeMapper
import org.radarbase.upload.doa.entity.SourceType
import org.radarbase.upload.inject.transact
import org.slf4j.LoggerFactory
import javax.persistence.EntityManager
import javax.ws.rs.core.Context


class SourceTypeRepositoryImpl(
        @Context private var em: EntityManager,
        @Context private var config: Config,
        @Context private val sourceTypeMapper: SourceTypeMapper) : SourceTypeRepository {

    init {
        if (!isInitialized) {
            config.sourceTypes?.storeSourceTypesFromConfigs()
        }
    }

    private fun List<SourceTypeDTO>.storeSourceTypesFromConfigs() {
        this.forEach {
            logger.info("Initializing source-type repository")
            val sourceType = sourceTypeMapper.toSourceType(it)
            em.transact {
                val queryString = "SELECT s FROM SourceType s JOIN FETCH s.configuration WHERE s.name = :name"

                val result = createQuery(queryString, SourceType::class.java).run {
                    setParameter("name", sourceType.name)
                    resultList.firstOrNull()
                }
                if (result != null) {
                    logger.info("Updating source-type with name ${sourceType.name}")
                    merge<SourceType>(result.copy(sourceType))
                } else {
                    logger.info("Creating source-type with name ${sourceType.name}")
                    persist(sourceType)
                }
            }
        }
        isInitialized = true
    }

    fun SourceType.copy(sourceType: SourceType): SourceType {
        this.topics = sourceType.topics
        this.timeRequired = sourceType.timeRequired
        this.sourceIdRequired = sourceType.sourceIdRequired
        this.contentTypes = sourceType.contentTypes
        this.configuration = sourceType.configuration
        return this
    }


    override fun create(record: SourceType) = em.transact { persist(record) }

    override fun readAll(limit: Int?, lastId: Long?, detailed: Boolean): List<SourceType> {
        var queryString = "SELECT s FROM SourceType s"
        lastId?.let {
            queryString += " WHERE s.id > :lastId "
        }
        if (detailed) {
            queryString += " JOIN FETCH s.configuration"
        }
        queryString += " ORDER BY s.id"

        return em.transact {
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

    override fun read(name: String): SourceType? = em.transact {
        val queryString = "SELECT s FROM SourceType s JOIN FETCH s.configuration WHERE s.name = :name"

        createQuery(queryString, SourceType::class.java).run {
            setParameter("name", name)
            logger.info("Query Source type with name $name : {}", parameters)
            resultList.firstOrNull()
        }
    }

    override fun update(record: SourceType): SourceType = em.transact { merge<SourceType>(record) }

    override fun delete(record: SourceType) = em.transact { remove(record) }

    companion object {
        private val logger = LoggerFactory.getLogger(SourceTypeRepositoryImpl::class.java)
        private var isInitialized = false
    }
}
