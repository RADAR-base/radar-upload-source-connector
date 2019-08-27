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

package org.radarbase.upload.auth

import org.radarbase.upload.filter.AuthenticationFilter
import org.radarcns.auth.exception.TokenValidationException
import javax.ws.rs.NotAuthorizedException
import javax.ws.rs.container.ContainerRequestContext

interface AuthValidator {
    @Throws(TokenValidationException::class, NotAuthorizedException::class)
    fun verify(request: ContainerRequestContext): Auth?

    fun getToken(request: ContainerRequestContext): String? {
        val authorizationHeader = request.getHeaderString("Authorization")

        // Check if the HTTP Authorization header is present and formatted correctly
        if (authorizationHeader == null
                || !authorizationHeader.startsWith(AuthenticationFilter.BEARER, ignoreCase = true)) {
            return null
        }

        // Extract the token from the HTTP Authorization header
        return authorizationHeader.substring(AuthenticationFilter.BEARER.length).trim { it <= ' ' }
    }
}
