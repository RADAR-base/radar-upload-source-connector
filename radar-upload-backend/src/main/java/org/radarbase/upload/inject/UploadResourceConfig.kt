package org.radarbase.upload.inject

import org.glassfish.jersey.internal.inject.AbstractBinder
import org.glassfish.jersey.process.internal.RequestScoped
import org.glassfish.jersey.server.ResourceConfig
import org.radarbase.upload.Config
import org.radarbase.upload.auth.Auth
import org.radarbase.upload.doa.RecordRepository
import org.radarbase.upload.doa.RecordRepositoryImpl
import org.radarbase.upload.doa.SourceTypeRepository
import org.radarbase.upload.doa.SourceTypeRepositoryImpl
import org.radarbase.upload.dto.RecordMapper
import org.radarbase.upload.dto.RecordMapperImpl
import javax.inject.Singleton
import javax.persistence.EntityManager

abstract class UploadResourceConfig {
    fun resources(config: Config): ResourceConfig {
        val resources = ResourceConfig().apply {
            packages(
                    "org.radarbase.upload.auth",
                    "org.radarbase.upload.exception",
                    "org.radarbase.upload.filter",
                    "org.radarbase.upload.resource")
            register(binder(config))
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

            // Bind factories.
            bindFactory(AuthFactory::class.java)
                    .proxy(true)
                    .proxyForSameScope(false)
                    .to(Auth::class.java)
                    .`in`(RequestScoped::class.java)

            bind(DoaEntityManagerFactory::class.java)
                    .to(EntityManager::class.java)
                    .`in`(Singleton::class.java)

            bind(RecordMapperImpl::class.java)
                    .to(RecordMapper::class.java)

            bind(RecordRepositoryImpl::class.java)
                    .to(RecordRepository::class.java)
            bind(SourceTypeRepositoryImpl::class.java)
                    .to(SourceTypeRepository::class.java)
            registerAuthenticationUtilities(this)
        }
    }
}
