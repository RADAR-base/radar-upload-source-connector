package org.radarbase.upload.exception

import javax.ws.rs.ClientErrorException
import javax.ws.rs.core.Response

class ConflictException(val messageText: String) :
        ClientErrorException(messageText, Response.Status.CONFLICT) {

}
