package org.radarbase.upload.lifecycle

import org.glassfish.jersey.server.BackgroundScheduler
import org.glassfish.jersey.server.monitoring.ApplicationEvent
import org.glassfish.jersey.server.monitoring.ApplicationEventListener
import org.glassfish.jersey.server.monitoring.RequestEvent
import org.glassfish.jersey.server.monitoring.RequestEventListener
import org.radarbase.upload.Config
import org.radarbase.upload.doa.RecordRepository
import org.radarbase.upload.doa.RecordRepositoryImpl
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.persistence.EntityManagerFactory
import javax.ws.rs.core.Context
import javax.ws.rs.ext.Provider

@Provider
class RecordStateLifecycleManager(
        @BackgroundScheduler @Context private val executor: ScheduledExecutorService,
        @Context private val entityManagerFactory: EntityManagerFactory,
        @Context config: Config
) : ApplicationEventListener {
    private val staleProcessingAge: Map<String, Pair<Duration, Any>>

    init {
        val defaultStaleProcessingAge: Duration = Duration.ofMinutes(config.resetProcessingStatusTimeoutMin)
        staleProcessingAge = config.sourceTypes
                ?.map { source ->
                    val age = source.resetProcessingStatusTimeoutMin?.let { Duration.ofMinutes(it) }
                            ?: defaultStaleProcessingAge
                    source.name to (age to Any())
                }
                ?.toMap()
                ?: mapOf()
    }

    private var checkTask: List<Future<*>>? = null

    override fun onEvent(event: ApplicationEvent?) {
        event ?: return
        when (event.type) {
            ApplicationEvent.Type.INITIALIZATION_APP_FINISHED -> startStaleChecks()
            ApplicationEvent.Type.DESTROY_FINISHED -> cancelStaleChecks()
            else -> {}  // do nothing
        }
    }

    @Synchronized
    private fun cancelStaleChecks() {
        checkTask?.let { tasks ->
            tasks.forEach { it.cancel(true) }
            checkTask = null
        }
    }

    @Synchronized
    private fun startStaleChecks() {
        if (checkTask != null) {
            return
        }
        checkTask = staleProcessingAge
                .map { (source, ageLock) ->
                    val (age, lock) = ageLock
                    val delay = age.dividedBy(4)
                    executor.scheduleAtFixedRate({ runStaleCheck(source, age, lock) }, 0, delay.toSeconds(), TimeUnit.SECONDS)
                }
    }

    private inline fun <T> useRecordRepository(method: (RecordRepository) -> T): T {
        val entityManager = entityManagerFactory.createEntityManager()
        return try {
            method(RecordRepositoryImpl(javax.inject.Provider { entityManager }))
        } finally {
            entityManager.close()
        }
    }

    private fun runStaleCheck(source: String, age: Duration, lock: Any) {
        useRecordRepository { recordRepository ->
            try {
                synchronized(lock) {
                    logger.debug("Resetting stale PROCESSING records to READY for source {}.", source)
                    val numUpdated = recordRepository.resetStaleProcessing(source, age)
                    if (numUpdated == 0) {
                        logger.debug("Did not reset any PROCESSING records to READY for source {}.", source)
                    } else {
                        logger.info("Reset {} PROCESSING records to READY for source {}.", numUpdated, source)
                    }
                }
            } catch (ex: Exception) {
                logger.error("Failed to run reset of stale processing", ex)
            }
        }
    }

    override fun onRequest(requestEvent: RequestEvent?): RequestEventListener? = null

    companion object {
        private val logger = LoggerFactory.getLogger(RecordStateLifecycleManager::class.java)
    }
}
