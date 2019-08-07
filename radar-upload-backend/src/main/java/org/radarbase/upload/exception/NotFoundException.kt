package org.radarbase.upload.exception

import org.radarbase.auth.jersey.exception.HttpApplicationException
import javax.ws.rs.core.Response

class NotFoundException(code: String, message: String) :
        HttpApplicationException(Response.Status.NOT_FOUND, code, message)
