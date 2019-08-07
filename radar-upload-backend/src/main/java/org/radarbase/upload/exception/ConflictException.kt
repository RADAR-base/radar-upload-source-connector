package org.radarbase.upload.exception

import org.radarbase.appconfig.exception.HttpApplicationException
import javax.ws.rs.ClientErrorException
import javax.ws.rs.core.Response

class ConflictException(code: String, messageText: String) :
        HttpApplicationException(Response.Status.CONFLICT, code, messageText)
