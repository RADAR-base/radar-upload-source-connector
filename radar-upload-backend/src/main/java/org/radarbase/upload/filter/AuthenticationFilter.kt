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

package org.radarbase.upload.filter

import org.radarbase.upload.auth.AuthValidator
import org.radarbase.upload.auth.Authenticated
import org.radarbase.upload.auth.RadarSecurityContext
import org.radarcns.auth.exception.TokenValidationException
import org.slf4j.LoggerFactory
import javax.annotation.Priority
import javax.ws.rs.Priorities
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response
import javax.ws.rs.ext.Provider

/**
 * Authenticates user by a JWT in the bearer signed by the Management Portal.
 */
@Provider
@Authenticated
@Priority(Priorities.AUTHENTICATION)
class AuthenticationFilter : ContainerRequestFilter {

    @Context
    private lateinit var validator: AuthValidator

    override fun filter(requestContext: ContainerRequestContext) {
        val radarToken = try {
            validator.verify(requestContext)
        } catch (ex: TokenValidationException) {
            logger.warn("[401] {}: {}", requestContext.uriInfo.path, ex.message, ex)
            requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .header("WWW-Authenticate",
                                    BEARER_REALM
                                            + " error=\"invalid_token\""
                                            + " error_description=\"${ex.message}\"")
                            .build())
            null
        }
        logger.debug("Verified token : $radarToken for request ${requestContext.uriInfo.path}" )
        if (radarToken == null) {
            logger.debug("[401] {}: Could not find a valid token in the header",
                    requestContext.uriInfo.path)
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .header("WWW-Authenticate", BEARER_REALM)
                    .build())
        } else {
            requestContext.securityContext = RadarSecurityContext(radarToken)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AuthenticationFilter::class.java)

        const val BEARER_REALM: String = "Bearer realm=\"Upload server\""
        const val BEARER: String = "Bearer "
    }
}
