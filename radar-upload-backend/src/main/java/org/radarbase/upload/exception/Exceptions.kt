package org.radarbase.upload.exception

import java.lang.RuntimeException
import javax.ws.rs.core.Response.Status

open class HttpApplicationException(val status: Int, val code: String, val detailedMessage: String?) : RuntimeException("[$status] $code: $detailedMessage") {
    constructor(status: Status, code: String, detailedMessage: String?) : this(status.statusCode, code, detailedMessage)
}

class BadGatewayException(message: String) :
        HttpApplicationException(Status.BAD_GATEWAY, "bad_gateway", message)

class ConflictException(code: String, messageText: String) :
        HttpApplicationException(Status.CONFLICT, code, messageText)

class NotAuthorizedException(code: String, message: String)
    : HttpApplicationException(Status.UNAUTHORIZED, code, message)

class NotFoundException(code: String, message: String) :
        HttpApplicationException(Status.NOT_FOUND, code, message)

class InternalServerException(code: String, message: String) :
        HttpApplicationException(Status.INTERNAL_SERVER_ERROR, code, message)
