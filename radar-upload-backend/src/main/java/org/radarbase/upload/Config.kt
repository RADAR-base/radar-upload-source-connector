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

package org.radarbase.upload

import org.radarbase.jersey.enhancer.EnhancerFactory
import org.radarbase.upload.api.SourceTypeDTO
import org.radarbase.upload.inject.ManagementPortalEnhancerFactory
import java.net.URI

data class Config(
    val baseUri: URI = URI.create("http://0.0.0.0:8085/upload/api/"),
    val advertisedBaseUri: URI? = null,
    val managementPortalUrl: String = "http://managementportal-app:8080/managementportal/",
    val resourceConfig: Class<out EnhancerFactory> = ManagementPortalEnhancerFactory::class.java,
    val clientId: String = "UploadBackend",
    val clientSecret: String? = null,
    val jdbcDriver: String? = "org.postgresql.Driver",
    val jdbcUrl: String? = null,
    val jdbcUser: String? = null,
    val jdbcPassword: String? = null,
    val hibernateDialect: String = "org.hibernate.dialect.PostgreSQL95Dialect",
    val jwtECPublicKeys: List<String>? = null,
    val jwtRSAPublicKeys: List<String>? = null,
    val jwtIssuer: String? = null,
    val jwtResourceName: String = "res_upload",
    val sourceTypes: List<SourceTypeDTO>? = null,
    val additionalPersistenceConfig: Map<String, String>? = null,
    val contentStreamBufferSize: Long = 1048576L,
    val enableCors: Boolean? = false,
    val syncProjectsIntervalMin: Long = 30,
    val syncParticipantsIntervalMin: Long = 30,
    val resetProcessingStatusTimeoutMin: Long = 300,
)
