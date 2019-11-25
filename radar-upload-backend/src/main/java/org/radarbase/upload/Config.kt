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

import org.radarbase.jersey.config.EnhancerFactory
import org.radarbase.upload.api.SourceTypeDTO
import org.radarbase.upload.inject.ManagementPortalEnhancerFactory
import java.net.URI

data class Config(
        var baseUri: URI = URI.create("http://0.0.0.0:8085/upload/api/"),
        var advertisedBaseUri: URI? = null,
        var managementPortalUrl: String = "http://managementportal-app:8080/managementportal/",
        var resourceConfig: Class<out EnhancerFactory> = ManagementPortalEnhancerFactory::class.java,
        var clientId: String = "UploadBackend",
        var clientSecret: String? = null,
        var jdbcDriver: String? = "org.h2.Driver",
        var jdbcUrl: String? = null,
        var jdbcUser: String? = null,
        var jdbcPassword: String? = null,
        var jwtECPublicKeys: List<String>? = null,
        var jwtRSAPublicKeys: List<String>? = null,
        var jwtIssuer: String? = null,
        var jwtResourceName: String = "res_upload",
        var sourceTypes: List<SourceTypeDTO>? = null,
        var additionalPersistenceConfig: Map<String, String>? = null,
        var contentStreamBufferSize: Long = 1048576L,
        var enableCors: Boolean? = false)
