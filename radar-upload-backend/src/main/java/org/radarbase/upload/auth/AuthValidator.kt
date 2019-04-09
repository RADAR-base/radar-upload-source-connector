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
