package org.radarbase.upload.resource

import org.radarbase.upload.Config
import org.radarbase.upload.auth.Auth
import org.radarbase.upload.auth.Authenticated
import java.net.URI
import javax.annotation.Resource
import javax.ws.rs.*
import javax.ws.rs.core.*

@Authenticated
@Path("/login")
@Resource
class LoginResource {
    @GET
    fun login(@QueryParam("redirect") redirect: String?, @Context auth: Auth, @Context config: Config, @Context uri: UriInfo): Response {
        val token = auth.bearerToken
                ?: throw BadRequestException("Cannot log in without bearer token")

        val myUri = config.advertisedBaseUri ?: uri.baseUri

        val responseBuilder = redirect?.let { Response.temporaryRedirect(URI.create(it)) }
                ?: Response.noContent()

        return responseBuilder
                .cookie(NewCookie("authorizationBearer", token, myUri.path, myUri.host, null, -1, true, true))
                .build()
    }
}
