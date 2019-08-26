package org.radarbase.upload.inject

import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.exception.LiquibaseException
import liquibase.resource.ClassLoaderResourceAccessor
import org.glassfish.jersey.internal.inject.DisposableSupplier
import org.hibernate.Session
import org.hibernate.internal.SessionImpl
import org.radarbase.upload.logger
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.util.concurrent.atomic.AtomicBoolean
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.ws.rs.core.Context

class DoaEntityManagerFactory(@Context private val emf: EntityManagerFactory) : DisposableSupplier<EntityManager> {
    override fun get(): EntityManager {
        logger.debug("Creating EntityManager...")
        return emf.createEntityManager()
    }

    override fun dispose(instance: EntityManager?) {
        logger.debug("Disposing  EntityManager")
        instance?.close()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DoaEntityManagerFactory::class.java)
    }
}

fun <T> EntityManager.transact(transaction: EntityManager.() -> T): T {
    getTransaction().begin()
    try {
        return transaction()
    } finally {
        try {
            getTransaction().commit()
        } catch (exe: Exception) {
            logger.warn("Rolling back operation: {}", exe.toString())
            getTransaction().rollback()
        }
    }
}

val EntityManager.session: Session
    get() = unwrap(Session::class.java)
