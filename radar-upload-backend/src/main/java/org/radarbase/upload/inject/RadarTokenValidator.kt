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

import org.radarbase.upload.Config
import org.radarbase.upload.auth.Auth
import org.radarbase.upload.auth.AuthValidator
import org.radarbase.upload.auth.ManagementPortalAuth
import org.radarcns.auth.authentication.TokenValidator
import org.radarcns.auth.config.TokenVerifierPublicKeyConfig
import org.slf4j.LoggerFactory
import java.io.IOException
import java.lang.Exception
import java.net.URI
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.Context

/** Creates a TokenValidator based on the current management portal configuration. */
class RadarTokenValidator constructor(@Context config: Config) : AuthValidator {
    private val tokenValidator = try {
        TokenValidator()
    } catch (e: RuntimeException) {
        TokenValidator(TokenVerifierPublicKeyConfig().apply {
            publicKeyEndpoints = listOf(URI("${config.managementPortalUrl}/oauth/token_key"))
            resourceName = config.jwtResourceName
        })
    }

    init {
        try {
            this.tokenValidator.refresh()
            logger.info("Refreshed Token Validator")
        } catch (ex: Exception) {
            logger.error("Failed to immediatly initialize token validator, will try again later: {}",
                    ex.toString())
        }
    }

    override fun verify(request: ContainerRequestContext): Auth? {
        return getToken(request)?.let {
            ManagementPortalAuth(tokenValidator.validateAccessToken(it))
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RadarTokenValidator::class.java)
    }
}
