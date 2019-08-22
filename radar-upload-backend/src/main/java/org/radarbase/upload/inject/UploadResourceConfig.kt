package org.radarbase.upload.inject

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import okhttp3.OkHttpClient
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.glassfish.jersey.process.internal.RequestScoped
import org.glassfish.jersey.server.ResourceConfig
import org.radarbase.upload.Config
import org.radarbase.upload.api.RecordMapper
import org.radarbase.upload.api.RecordMapperImpl
import org.radarbase.upload.api.SourceTypeMapper
import org.radarbase.upload.api.SourceTypeMapperImpl
import org.radarbase.upload.auth.Auth
import org.radarbase.upload.auth.MPClient
import org.radarbase.upload.doa.RecordRepository
import org.radarbase.upload.doa.RecordRepositoryImpl
import org.radarbase.upload.doa.SourceTypeRepository
import org.radarbase.upload.doa.SourceTypeRepositoryImpl
import org.radarbase.upload.dto.CallbackManager
import org.radarbase.upload.dto.QueuedCallbackManager
import org.radarbase.upload.service.MPService
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.ws.rs.ext.ContextResolver

abstract class UploadResourceConfig {
    private val client = OkHttpClient().newBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    fun resources(config: Config): ResourceConfig {
        val resources = ResourceConfig().apply {
            packages(
                    "org.radarbase.upload.auth",
                    "org.radarbase.upload.exception",
                    "org.radarbase.upload.filter",
                    "org.radarbase.upload.resource")
            register(binder(config))
            register(ContextResolver<ObjectMapper> { OBJECT_MAPPER })
            property("jersey.config.server.wadl.disableWadl", true)
        }
        registerAuthentication(resources)
        return resources
    }

    abstract fun registerAuthentication(resources: ResourceConfig)

    abstract fun registerAuthenticationUtilities(binder: AbstractBinder)

    private fun binder(config: Config) = object : AbstractBinder() {
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

            bind(MPClient::class.java)
                    .to(MPClient::class.java)
                    .`in`(Singleton::class.java)

            bind(MPService::class.java)
                    .to(MPService::class.java)
                    .`in`(Singleton::class.java)

            // Bind factories.
            bindFactory(AuthFactory::class.java)
                    .proxy(true)
                    .proxyForSameScope(true)
                    .to(Auth::class.java)
                    .`in`(RequestScoped::class.java)

            bindFactory(DoaEntityManagerFactoryFactory::class.java)
                    .to(EntityManagerFactory::class.java)
                    .`in`(Singleton::class.java)

            bindFactory(DoaEntityManagerFactory::class.java)
                    .to(EntityManager::class.java)
                    .`in`(RequestScoped::class.java)

            bind(RecordMapperImpl::class.java)
                    .to(RecordMapper::class.java)
                    .`in`(Singleton::class.java)

            bind(SourceTypeMapperImpl::class.java)
                    .to(SourceTypeMapper::class.java)
                    .`in`(Singleton::class.java)

            bind(RecordRepositoryImpl::class.java)
                    .to(RecordRepository::class.java)
                    .`in`(Singleton::class.java)

            bind(SourceTypeRepositoryImpl::class.java)
                    .to(SourceTypeRepository::class.java)
                    .`in`(Singleton::class.java)

            registerAuthenticationUtilities(this)
        }
    }

    companion object {
        private val OBJECT_MAPPER = ObjectMapper()
                .registerModule(JavaTimeModule())
                .registerModule(KotlinModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }
}
