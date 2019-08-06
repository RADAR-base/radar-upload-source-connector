package org.radarbase.upload.exception

import org.radarbase.appconfig.exception.HttpApplicationException
import javax.ws.rs.core.Response.Status

class BadGatewayException(message: String) : HttpApplicationException(Status.BAD_GATEWAY, "bad_gateway", message)
