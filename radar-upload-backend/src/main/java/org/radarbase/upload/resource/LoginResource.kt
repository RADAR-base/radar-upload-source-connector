package org.radarbase.upload.resource

import org.radarbase.auth.jersey.Auth
import org.radarbase.auth.jersey.Authenticated
import org.radarbase.upload.Config
import java.net.URI
import java.time.Duration
import java.time.Instant
import javax.annotation.Resource
import javax.ws.rs.*
import javax.ws.rs.core.*

@Path("/")
@Resource
@Produces("application/json")
class LoginResource {
    @GET
    @Path("login")
    @Authenticated
    fun login(@QueryParam("redirect") redirect: String?, @Context auth: Auth, @Context config: Config, @Context uri: UriInfo): Response {
        val token = auth.token.token
                ?: throw BadRequestException("Cannot log in without bearer token")

        val myUri = config.advertisedBaseUri ?: uri.baseUri

        val responseBuilder = redirect?.let { Response.temporaryRedirect(URI.create(it)) }
                ?: Response.ok().entity(mapOf("authorizationBearer" to token))

        val age = auth.token.expiresAt?.let { expiry ->
            Duration.between(Instant.now(), expiry.toInstant()).toSeconds().toInt()
        }?.coerceAtLeast(0) ?: -1

        return responseBuilder
                .cookie(NewCookie("authorizationBearer", token, myUri.path, myUri.host, null, age, true, true))
                .build()
    }

    @GET
    @Path("logout")
    fun logout(@QueryParam("redirect") redirect: String?, @Context config: Config, @Context uri: UriInfo): Response {
        val myUri = config.advertisedBaseUri ?: uri.baseUri

        val responseBuilder = redirect?.let { Response.temporaryRedirect(URI.create(it)) }
                ?: Response.ok().entity(mapOf("authorizationBearer" to null))

        return responseBuilder
                .cookie(NewCookie("authorizationBearer", "", myUri.path, myUri.host, null, 0, true, true))
                .build()
    }
}
