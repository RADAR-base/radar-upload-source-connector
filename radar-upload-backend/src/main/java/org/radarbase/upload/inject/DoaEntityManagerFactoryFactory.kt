package org.radarbase.upload.inject

import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.exception.LiquibaseException
import liquibase.resource.ClassLoaderResourceAccessor
import org.glassfish.jersey.internal.inject.DisposableSupplier
import org.hibernate.internal.SessionImpl
import org.radarbase.upload.Config
import org.slf4j.LoggerFactory
import javax.persistence.EntityManagerFactory
import javax.persistence.Persistence
import javax.ws.rs.core.Context

class DoaEntityManagerFactoryFactory(@Context config: Config) : DisposableSupplier<EntityManagerFactory> {
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

    override fun get(): EntityManagerFactory {
        logger.info("Initializing EntityManagerFactory with config: $configMap")
        val emf = Persistence.createEntityManagerFactory("org.radarbase.upload.doa", configMap)
        logger.info("Initializing Liquibase")
        val connection = emf.createEntityManager().unwrap(SessionImpl::class.java).connection()
        try {
            val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(connection))
            val liquibase = Liquibase("dbChangelog.xml", ClassLoaderResourceAccessor(), database)
            liquibase.update("test")
        } catch (e: LiquibaseException) {
            e.printStackTrace()
        }
        return emf
    }

    override fun dispose(instance: EntityManagerFactory?) {
        logger.info("Disposing EntityManagerFactory")
        instance?.close()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DoaEntityManagerFactoryFactory::class.java)
    }

}
