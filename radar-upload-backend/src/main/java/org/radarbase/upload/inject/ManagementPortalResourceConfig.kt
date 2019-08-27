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

import org.glassfish.jersey.internal.inject.AbstractBinder
import org.glassfish.jersey.server.ResourceConfig
import org.radarbase.upload.auth.AuthValidator
import javax.inject.Singleton

/** This binder needs to register all non-Jersey classes, otherwise initialization fails. */
class ManagementPortalResourceConfig : UploadResourceConfig() {
    override fun registerAuthentication(resources: ResourceConfig) {
        // none needed
    }

    override fun registerAuthenticationUtilities(binder: AbstractBinder) {
        binder.bind(RadarTokenValidator::class.java)
                .to(AuthValidator::class.java)
                .`in`(Singleton::class.java)
    }
}
