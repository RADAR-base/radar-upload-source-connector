/*
 *
 *  * Copyright 2019 The Hyve
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */

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
