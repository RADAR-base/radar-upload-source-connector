package org.radarbase.upload.inject

import org.glassfish.jersey.internal.inject.DisposableSupplier
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
        logger.info("Initializing EntityManagerFactory with config: ${configMap}")
        return Persistence.createEntityManagerFactory("org.radarbase.upload.doa", configMap)
    }

    override fun dispose(instance: EntityManagerFactory?) {
        logger.info("Disposing EntityManagerFactory")
        instance?.close()
    }

    companion object {
        val logger = LoggerFactory.getLogger(DoaEntityManagerFactoryFactory::class.java)
    }

}
