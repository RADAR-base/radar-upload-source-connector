package org.radarbase.upload.inject

import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.exception.LiquibaseException
import liquibase.resource.ClassLoaderResourceAccessor
import org.glassfish.jersey.internal.inject.DisposableSupplier
import org.hibernate.Session
import org.hibernate.internal.SessionImpl
import org.radarbase.upload.Config
import javax.persistence.EntityManager
import javax.persistence.Persistence
import javax.ws.rs.core.Context

class DoaEntityManagerFactory(@Context config: Config) : DisposableSupplier<EntityManager> {
    private val configMap: Map<String, String>

    init {
        configMap = HashMap()
        config.jdbcDriver?.let {
            configMap["javax.persistence.jdbc.driver"] = it
        }
        config.jdbcUrl?.let {
            configMap["javax.persistence.jdbc.url"] = it
        }
        config.jdbcUser?.let {
            configMap["javax.persistence.jdbc.user"] = it
        }
        config.jdbcPassword?.let {
            configMap["javax.persistence.jdbc.password"] = it
        }
        config.additionalPersistenceConfig?.let {
            it.map { entry -> configMap[entry.key] = entry.value}
        }
    }

    override fun get(): EntityManager {
        val emf = Persistence.createEntityManagerFactory("org.radarbase.upload.doa", configMap)
        val em = emf.createEntityManager()

        val connection = em.unwrap(SessionImpl::class.java).connection()

        try {
            val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(connection))
            val liquibase = Liquibase("dbChangelog.xml", ClassLoaderResourceAccessor(), database)
            liquibase.update("test")
        } catch (e: LiquibaseException) {
            e.printStackTrace()
        }

        return em
    }

    override fun dispose(instance: EntityManager?) {
        instance?.entityManagerFactory?.close()
    }
}

fun <T> EntityManager.transact(transaction: EntityManager.() -> T): T {
    getTransaction().begin()
    try {
        return transaction()
    } finally {
        getTransaction().commit()
    }
}

val EntityManager.session: Session
    get() = unwrap(Session::class.java)
