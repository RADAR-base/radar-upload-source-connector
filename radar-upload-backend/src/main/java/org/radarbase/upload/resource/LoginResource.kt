package org.radarbase.upload.resource

import org.radarbase.upload.Config
import org.radarbase.upload.auth.Auth
import org.radarbase.upload.auth.Authenticated
import javax.annotation.Resource
import javax.ws.rs.BadRequestException
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.core.*

@Authenticated
@Path("/login")
@Resource
class LoginResource {
    @POST
    fun login(@Context auth: Auth, @Context config: Config, @Context uri: UriInfo): Response {
        val token = auth.bearerToken
                ?: throw BadRequestException("Cannot log in without bearer token")

        val myUri = config.advertisedBaseUri ?: uri.baseUri

        return Response.noContent()
                .cookie(NewCookie("authorizationBearer", token, myUri.path, myUri.host, null, -1, true, true))
                .build()
    }
}
