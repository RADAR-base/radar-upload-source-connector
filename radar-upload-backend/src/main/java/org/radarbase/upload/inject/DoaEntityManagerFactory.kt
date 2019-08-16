package org.radarbase.upload.inject

import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.exception.LiquibaseException
import liquibase.resource.ClassLoaderResourceAccessor
import org.glassfish.jersey.internal.inject.DisposableSupplier
import org.hibernate.Session
import org.hibernate.internal.SessionImpl
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.util.concurrent.atomic.AtomicBoolean
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.ws.rs.core.Context

class DoaEntityManagerFactory(@Context emf: EntityManagerFactory) : DisposableSupplier<EntityManager> {
    private val emfactory: EntityManagerFactory = emf
    override fun get(): EntityManager {
        logger.debug("Creating EntityManager...")
        val em = emfactory.createEntityManager()

        if(!isLiquibaseInitialized.get()) {
            logger.info("Initializing Liquibase")
            val connection = em.unwrap(SessionImpl::class.java).connection()
            try {
                val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(connection))
                val liquibase = Liquibase("dbChangelog.xml", ClassLoaderResourceAccessor(), database)
                liquibase.update("test")
            } catch (e: LiquibaseException) {
                e.printStackTrace()
            }
            isLiquibaseInitialized.set(true)
        }

        return em
    }

    override fun dispose(instance: EntityManager?) {
        logger.debug("Disposing  EntityManager")
        instance?.close()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DoaEntityManagerFactory::class.java)
        private var isLiquibaseInitialized: AtomicBoolean = AtomicBoolean(false)
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

            getTransaction().rollback()
        }
    }
}

val EntityManager.session: Session
    get() = unwrap(Session::class.java)
