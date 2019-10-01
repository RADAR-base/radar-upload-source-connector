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

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import okhttp3.OkHttpClient
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.glassfish.jersey.process.internal.RequestScoped
import org.glassfish.jersey.server.ResourceConfig
import org.radarbase.auth.jersey.JerseyResourceEnhancer
import org.radarbase.auth.jersey.RadarJerseyResourceEnhancer
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

    fun resources(config: Config) = ResourceConfig().apply {
        val enhancers = createEnhancers(config)
        packages(
                "org.radarbase.upload.exception",
                "org.radarbase.upload.filter",
                "org.radarbase.upload.resource")
        enhancers.forEach { packages(*it.packages) }
        register(binder(config, enhancers))
        register(ContextResolver { OBJECT_MAPPER })
        property("jersey.config.server.wadl.disableWadl", true)
    }

    abstract fun createEnhancers(config: Config): List<JerseyResourceEnhancer>

    abstract fun registerAuthentication(binder: AbstractBinder, config: Config)

    private fun binder(config: Config, enhancers: List<JerseyResourceEnhancer>) = object : AbstractBinder() {
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

            enhancers.forEach { it.enhance(this) }

            registerAuthentication(this, config)
        }
    }

    companion object {
        private val OBJECT_MAPPER = ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .registerModule(JavaTimeModule())
                .registerModule(KotlinModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }
}
