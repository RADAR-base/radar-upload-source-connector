package org.radarbase.upload.doa

import org.radarbase.upload.doa.entity.SourceType
import org.radarbase.upload.inject.transact
import org.slf4j.LoggerFactory
import java.io.Closeable
import javax.persistence.EntityManager
import javax.persistence.Persistence
import javax.ws.rs.core.Context


class SourceTypeRepositoryImpl(@Context private var em: EntityManager) : SourceTypeRepository {
    override fun create(record: SourceType) = em.transact { persist(record) }

    override fun read(name: String): SourceType? = em.transact {
        val query = createQuery("SELECT s FROM SourceType s WHERE s.name = :name", SourceType::class.java)
        logger.info("QUERY PARAMETERS: {}", query.parameters)
        query.setParameter("name", name)
                .resultList
                .firstOrNull()
    }

    override fun readDetailed(name: String): SourceType? = em.transact {
        createQuery(
                """SELECT s FROM SourceType s JOIN FETCH s.configuration WHERE s.name = :name""")
                .setParameter("name", name)
                .resultList
                .firstOrNull() as SourceType
    }

    override fun update(record: SourceType): SourceType = em.transact { merge<SourceType>(record) }

    override fun delete(record: SourceType) = em.transact { remove(record) }

    companion object {
        private val logger = LoggerFactory.getLogger(SourceTypeRepositoryImpl::class.java)
    }
}
