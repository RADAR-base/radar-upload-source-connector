package org.radarbase.upload.inject

import org.glassfish.jersey.internal.inject.AbstractBinder
import org.radarbase.jersey.config.ConfigLoader
import org.radarbase.jersey.enhancer.JerseyResourceEnhancer
import org.radarbase.jersey.filter.Filters
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
import jakarta.inject.Singleton

class UploadResourceEnhancer(private val config: Config): JerseyResourceEnhancer {
    override val classes: Array<Class<*>>  get() {
        return if (config.enableCors == true) {
            arrayOf(
                Filters.logResponse,
                Filters.cache,
                Filters.cors,
            )
        } else {
            arrayOf(
                Filters.logResponse,
                Filters.cache,
            )
        }
    }

    override val packages: Array<String> = arrayOf(
        "org.radarbase.upload.exception",
        "org.radarbase.upload.filter",
        "org.radarbase.upload.lifecycle",
        "org.radarbase.upload.resource"
    )

    override fun AbstractBinder.enhance() {
        // Bind instances. These cannot use any injects themselves
        bind(config)
                .to(Config::class.java)

        bind(QueuedCallbackManager::class.java)
                .to(CallbackManager::class.java)
                .`in`(Singleton::class.java)

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
    }
}
