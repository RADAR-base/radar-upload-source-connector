package org.radarbase.upload.inject

import org.glassfish.jersey.internal.inject.DisposableSupplier
import org.hibernate.Session
import org.radarbase.upload.logger
import org.slf4j.LoggerFactory
import java.lang.Exception
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
            logger.error("Rolling back operation: {}", exe)
            if(getTransaction() != null && getTransaction().isActive) {
                getTransaction().rollback()
            }
        }
    }
}

val EntityManager.session: Session
    get() = unwrap(Session::class.java)
