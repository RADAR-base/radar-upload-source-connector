package org.radarbase.upload.exception

import org.radarbase.appconfig.exception.HttpApplicationException
import javax.ws.rs.core.Response

class NotAuthorizedException(code: String, message: String)
    : HttpApplicationException(Response.Status.UNAUTHORIZED, code, message)

