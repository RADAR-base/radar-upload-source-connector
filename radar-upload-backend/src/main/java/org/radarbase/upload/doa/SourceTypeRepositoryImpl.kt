package org.radarbase.upload.doa

import org.radarbase.upload.doa.entity.SourceType
import org.radarbase.upload.inject.transact
import org.slf4j.LoggerFactory
import javax.persistence.EntityManager
import javax.ws.rs.core.Context


class SourceTypeRepositoryImpl(@Context private var em: EntityManager) : SourceTypeRepository {

    override fun create(record: SourceType) = em.transact { persist(record) }

    override fun read(limit: Int, name: String?, detailed: Boolean): List<SourceType>  {
        var queryString = "SELECT s FROM SourceType s"
        if (detailed) {
            queryString += " JOIN FETCH s.configuration"
        }
        name?.let {
            queryString += " WHERE  s.name = :name"
        }

        return em.transact {
            val query = createQuery(queryString, SourceType::class.java)
                    .setMaxResults(limit)
            name?.let {
                query.setParameter("name", it)
            }
            logger.info("QUERY PARAMETERS: {}", query.parameters)
            query.resultList
        }
    }

    override fun read(name: String): SourceType? = read(1, name, true).firstOrNull()

    override fun update(record: SourceType): SourceType = em.transact { merge<SourceType>(record) }

    override fun delete(record: SourceType) = em.transact { remove(record) }

    companion object {
        private val logger = LoggerFactory.getLogger(SourceTypeRepositoryImpl::class.java)
    }
}
