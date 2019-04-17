package org.radarbase.upload.doa

import org.radarbase.upload.doa.entity.SourceType
import org.radarbase.upload.inject.transact
import org.slf4j.LoggerFactory
import javax.persistence.EntityManager
import javax.ws.rs.core.Context


class SourceTypeRepositoryImpl(@Context private var em: EntityManager) : SourceTypeRepository {

    override fun create(record: SourceType) = em.transact { persist(record) }

    override fun readAll(limit: Int?, lastId: Long?, detailed: Boolean): List<SourceType>  {
        var queryString = "SELECT s FROM SourceType s WHERE s.id > :lastId "
        if (detailed) {
            queryString += " JOIN FETCH s.configuration"
        }
        queryString += "ORDER BY s.id"

        return em.transact {
            val query = createQuery(queryString, SourceType::class.java)
            limit?.let {
                query.setMaxResults(it)
            }
            lastId?.let {
                query.setParameter("lastId", it)
            }

            logger.info("QUERY PARAMETERS: {}", query.parameters)
            query.resultList
        }
    }

    override fun read(name: String): SourceType? = em.transact {
        val queryString = "SELECT s FROM SourceType s JOIN FETCH s.configuration WHERE  s.name = :name"

        val query = createQuery(queryString, SourceType::class.java)
                .setParameter("name", name)
        logger.info("Query Source type with name $name : {}", query.parameters)
        query.resultList.firstOrNull()
    }

    override fun update(record: SourceType): SourceType = em.transact { merge<SourceType>(record) }

    override fun delete(record: SourceType) = em.transact { remove(record) }

    companion object {
        private val logger = LoggerFactory.getLogger(SourceTypeRepositoryImpl::class.java)
    }
}
