package org.radarbase.upload.inject

import org.glassfish.jersey.internal.inject.DisposableSupplier
import org.hibernate.Session
import org.radarbase.upload.exception.InternalServerException
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

fun <T> EntityManager.transact(transactionOperation: EntityManager.() -> T): T {
    val currentTransaction = transaction ?: throw InternalServerException("transaction_not_found", "Cannot find a transaction from EntityManager")

    currentTransaction.begin()
    try {
        return transactionOperation()
    } finally {
        try {
            currentTransaction.commit()
        } catch (exe: Exception) {
            logger.error("Rolling back operation: {}", exe)
            if (currentTransaction.isActive) {
                currentTransaction.rollback()
            }
        }
    }
}

val EntityManager.session: Session
    get() = unwrap(Session::class.java)
