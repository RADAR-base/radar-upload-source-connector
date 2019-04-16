package org.radarbase.upload.resource

import org.radarbase.upload.auth.Authenticated
import javax.annotation.Resource
import javax.ws.rs.Consumes
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/source-types")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Resource
@Authenticated
class SourceTypeResourceResource {

}
