package org.radarbase.upload.resource

import org.radarbase.upload.auth.Auth
import org.radarbase.upload.auth.Authenticated
import org.radarbase.upload.doa.SourceTypeRepository
import org.radarbase.upload.api.SourceTypeContainerDTO
import org.radarbase.upload.api.SourceTypeDTO
import org.radarbase.upload.api.SourceTypeMapper
import javax.annotation.Resource
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType

@Path("/source-types")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Resource
@Authenticated
class SourceTypeResource {
    @Context
    lateinit var sourceTypeRepository: SourceTypeRepository

    @Context
    lateinit var auth: Auth

    @Context
    lateinit var sourceTypeMapper: SourceTypeMapper

    @GET
    fun query(@DefaultValue("20") @QueryParam("limit") limit: Int,
              @QueryParam("lastId") lastId: Long?): SourceTypeContainerDTO {

        val imposedLimit = Math.min(Math.max(limit, 1), 100)
        val records = sourceTypeRepository.readAll(imposedLimit, lastId)

        return sourceTypeMapper.fromSourceTypes(records)
    }

    @GET
    @Path("{name}")
    fun getSourceType(
            @PathParam("name") name: String): SourceTypeDTO {

        val record = sourceTypeRepository.read( name)
                ?: throw NotFoundException("Source type with name $name not found")

        return sourceTypeMapper.fromSourceType(record)
    }
}
