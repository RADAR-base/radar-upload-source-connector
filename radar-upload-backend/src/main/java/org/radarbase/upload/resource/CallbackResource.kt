package org.radarbase.upload.resource

import org.slf4j.LoggerFactory
import javax.annotation.Resource
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Path("callback")
@Produces(MediaType.APPLICATION_JSON)
@Resource
class CallbackResource {


    @GET
    fun getCode(@QueryParam("code") code: String?) {
        logger.debug("Code received is $code")
    }

    companion object {
        val logger = LoggerFactory.getLogger(CallbackResource::class.java)
    }

}
