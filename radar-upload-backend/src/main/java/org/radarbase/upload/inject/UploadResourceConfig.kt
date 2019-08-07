package org.radarbase.upload.inject

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import okhttp3.OkHttpClient
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.glassfish.jersey.server.ResourceConfig
import org.radarbase.upload.Config
import org.radarbase.upload.api.RecordMapper
import org.radarbase.upload.api.RecordMapperImpl
import org.radarbase.upload.api.SourceTypeMapper
import org.radarbase.upload.api.SourceTypeMapperImpl
import org.radarbase.upload.doa.RecordRepository
import org.radarbase.upload.doa.RecordRepositoryImpl
import org.radarbase.upload.doa.SourceTypeRepository
import org.radarbase.upload.doa.SourceTypeRepositoryImpl
import org.radarbase.upload.dto.CallbackManager
import org.radarbase.upload.dto.QueuedCallbackManager
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.persistence.EntityManager
import javax.ws.rs.ext.ContextResolver

abstract class UploadResourceConfig {
    private val client = OkHttpClient().newBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    private val OBJECT_MAPPER = ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerModule(KotlinModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

    fun resources(config: Config): ResourceConfig {
        val resources = ResourceConfig().apply {
            packages(
                    "org.radarbase.upload.auth",
                    "org.radarbase.upload.exception",
                    "org.radarbase.upload.filter",
                    "org.radarbase.upload.resource")
            register(binder(this, config))
            register(ContextResolver<ObjectMapper> {OBJECT_MAPPER})
            property("jersey.config.server.wadl.disableWadl", true)
        }
        return resources
    }

    abstract fun registerAuthentication(resources: ResourceConfig, binder: AbstractBinder, config: Config)

    private fun binder(resourceConfig: ResourceConfig, config: Config) = object : AbstractBinder() {
        override fun configure() {
            // Bind instances. These cannot use any injects themselves
            bind(config)
                    .to(Config::class.java)

            bind(client)
                    .to(OkHttpClient::class.java)

            bind(OBJECT_MAPPER)
                    .to(ObjectMapper::class.java)

            bind(QueuedCallbackManager::class.java)
                    .to(CallbackManager::class.java)
                    .`in`(Singleton::class.java)

            // Bind factories.
            bindFactory(DoaEntityManagerFactory::class.java)
                    .to(EntityManager::class.java)
                    .`in`(Singleton::class.java)

            bind(RecordMapperImpl::class.java)
                    .to(RecordMapper::class.java)

            bind(SourceTypeMapperImpl::class.java)
                    .to(SourceTypeMapper::class.java)

            bind(RecordRepositoryImpl::class.java)
                    .to(RecordRepository::class.java)

            bind(SourceTypeRepositoryImpl::class.java)
                    .to(SourceTypeRepository::class.java)

            registerAuthentication(resourceConfig, this, config)
        }
    }
}
