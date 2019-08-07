package org.radarbase.upload.exception

import org.radarbase.auth.jersey.exception.HttpApplicationException
import javax.ws.rs.core.Response

class ConflictException(code: String, messageText: String) :
        HttpApplicationException(Response.Status.CONFLICT, code, messageText)
