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

import org.glassfish.jersey.internal.inject.DisposableSupplier
import org.radarbase.upload.exception.InternalServerException
import org.radarbase.upload.logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.lang.Exception
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.EntityTransaction
import javax.ws.rs.core.Context

class DoaEntityManagerFactory(@Context private val emf: EntityManagerFactory) : DisposableSupplier<EntityManager> {
    override fun get(): EntityManager {
        logger.debug("Creating EntityManager...")
        return emf.createEntityManager()
    }

    override fun dispose(instance: EntityManager?) {
        instance?.let {
            logger.debug("Disposing  EntityManager")
            it.close()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DoaEntityManagerFactory::class.java)
    }
}

/**
 * Run a transaction and commit it. If an exception occurs, the transaction is rolled back.
 */
fun <T> EntityManager.transact(transactionOperation: EntityManager.() -> T) = createTransaction {
    it.use { transactionOperation() }
}

/**
 * Start a transaction without committing it. If an exception occurs, the transaction is rolled back.
 */
fun <T> EntityManager.createTransaction(transactionOperation: EntityManager.(CloseableTransaction) -> T): T {
    val currentTransaction = transaction ?: throw InternalServerException("transaction_not_found", "Cannot find a transaction from EntityManager")

    currentTransaction.begin()
    try {
        return transactionOperation(object : CloseableTransaction {
            override val transaction: EntityTransaction = currentTransaction

            override fun close() {
                try {
                    transaction.commit()
                } catch (ex: Exception) {
                    logger.error("Rolling back operation", ex)
                    if (currentTransaction.isActive) {
                        currentTransaction.rollback()
                    }
                }
            }
        })
    } catch (ex: Exception) {
        logger.error("Rolling back operation", ex)
        if (currentTransaction.isActive) {
            currentTransaction.rollback()
        }
        throw ex
    }
}

interface CloseableTransaction: Closeable {
    val transaction: EntityTransaction
    override fun close()
}
