package org.radarbase.upload.resource

import org.radarbase.upload.Config
import org.radarbase.upload.auth.Auth
import org.radarbase.upload.auth.Authenticated
import java.net.URI
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
        val token = auth.bearerToken
                ?: throw BadRequestException("Cannot log in without bearer token")

        val myUri = config.advertisedBaseUri ?: uri.baseUri

        val responseBuilder = redirect?.let { Response.temporaryRedirect(URI.create(it)) }
                ?: Response.ok().entity(mapOf("authorizationBearer" to token))

        return responseBuilder
                .cookie(NewCookie("authorizationBearer", token, myUri.path, myUri.host, null, -1, true, true))
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
