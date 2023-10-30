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

import org.radarbase.jersey.auth.AuthConfig
import org.radarbase.jersey.auth.MPConfig
import org.radarbase.jersey.enhancer.EnhancerFactory
import org.radarbase.jersey.enhancer.Enhancers
import org.radarbase.jersey.enhancer.JerseyResourceEnhancer
import org.radarbase.jersey.hibernate.config.DatabaseConfig
import org.radarbase.jersey.hibernate.config.HibernateResourceEnhancer
import org.radarbase.upload.Config
import org.radarbase.upload.doa.entity.Record
import org.radarbase.upload.doa.entity.RecordContent
import org.radarbase.upload.doa.entity.RecordLogs
import org.radarbase.upload.doa.entity.RecordMetadata
import org.radarbase.upload.doa.entity.SourceType
import kotlin.reflect.jvm.jvmName

/** This binder needs to register all non-Jersey classes, otherwise initialization fails. */
class ManagementPortalEnhancerFactory(private val config: Config) : EnhancerFactory {
    override fun createEnhancers(): List<JerseyResourceEnhancer> {
        val authConfig = AuthConfig(
            managementPortal = MPConfig(
                url = config.managementPortalUrl,
                clientId = config.clientId,
                clientSecret = config.clientSecret,
            ),
            jwtResourceName = config.jwtResourceName,
            jwtIssuer = config.jwtIssuer,
            jwtECPublicKeys = config.jwtECPublicKeys,
            jwtRSAPublicKeys = config.jwtRSAPublicKeys,
        )
        val dbConfig = DatabaseConfig(
            managedClasses = listOf(
                Record::class.jvmName,
                RecordMetadata::class.jvmName,
                RecordLogs::class.jvmName,
                RecordContent::class.jvmName,
                SourceType::class.jvmName,
            ),
            url = config.jdbcUrl,
            driver = config.jdbcDriver,
            user = config.jdbcUser,
            password = config.jdbcPassword,
            dialect = config.hibernateDialect,
            properties = config.additionalPersistenceConfig ?: emptyMap(),
        )
        return listOf(
            UploadResourceEnhancer(config),
            Enhancers.radar(authConfig),
            Enhancers.managementPortal(authConfig),
            Enhancers.health,
            HibernateResourceEnhancer(dbConfig),
            Enhancers.exception,
        )
    }
}
