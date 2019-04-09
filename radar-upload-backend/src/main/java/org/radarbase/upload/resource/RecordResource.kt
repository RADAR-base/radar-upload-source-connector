package org.radarbase.upload.resource

import org.radarbase.upload.auth.Auth
import org.radarbase.upload.auth.Authenticated
import org.radarbase.upload.auth.NeedsPermission
import org.radarbase.upload.doa.RecordRepository
import org.radarbase.upload.doa.entity.RecordStatus
import org.radarbase.upload.dto.ContentsDTO
import org.radarbase.upload.dto.RecordContainerDTO
import org.radarbase.upload.dto.RecordDTO
import org.radarbase.upload.dto.RecordMapper
import org.radarcns.auth.authorization.Permission
import org.radarcns.auth.authorization.Permission.PROJECT_READ
import org.radarcns.auth.authorization.Permission.SUBJECT_READ
import java.io.InputStream
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/records")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class RecordResource {

    @Context
    lateinit var recordRepository: RecordRepository

    @Context
    lateinit var auth: Auth

    @Context
    lateinit var recordMapper: RecordMapper

    @GET
    fun query(
            @QueryParam("projectId") projectId: String?,
            @QueryParam("userId") userId: String?,
            @QueryParam("limit") limit: Int?,
            @QueryParam("lastId") lastId: Long?,
            @QueryParam("status") status: String?): RecordContainerDTO {
        if (projectId == null) {
            throw BadRequestException("Required project ID not provided.")
        }

        if (userId != null) {
            auth.checkUserPermission(SUBJECT_READ, projectId, userId)
        } else {
            auth.checkProjectPermission(PROJECT_READ, projectId)
        }

        val imposedLimit = Math.min(limit ?: 10, 100)
        val records = recordRepository.query(imposedLimit, lastId ?: -1L, projectId, userId, status)

        return recordMapper.fromRecords(records, imposedLimit)
    }

    @POST
    @NeedsPermission(Permission.Entity.MEASUREMENT, Permission.Operation.CREATE)
    fun add(record: RecordDTO): RecordDTO {

        // TODO: logic to do authorization checking

        // TODO: more logic to do data checking

        if (record.id != null) {
            throw BadRequestException("Record ID cannot be set explicitly")
        }

        val doaRecord = recordMapper.toRecord(record)
        val result = recordRepository.create(doaRecord)
        return recordMapper.fromRecord(result)
    }

    @PUT
    @Consumes("*/*")
    @NeedsPermission(Permission.Entity.MEASUREMENT, Permission.Operation.CREATE)
    @Path("{recordId}/contents/{fileName}")
    fun putContents(
            input: InputStream,
            @HeaderParam("Content-Type") contentType: String,
            @HeaderParam("Content-Length") contentLength: Long,
            @PathParam("fileName") fileName: String,
            @PathParam("recordId") recordId: Long): ContentsDTO {

        // TODO: logic to do authorization checking

        val record = recordRepository.read(recordId)
                ?: throw NotFoundException("Record with ID $recordId does not exist")

        if (record.metadata.status != RecordStatus.INCOMPLETE) {
            throw WebApplicationException("Cannot add files to saved record.", Response.Status.CONFLICT)
        }

        val content = recordRepository.updateContent(record, fileName, contentType, input, contentLength)
        return recordMapper.fromContent(content)
    }

    // TODO: POLL path

    // TODO: get metadata path
    // TODO: update metadata path

    // TODO: read file contents path

    // TODO: read log contents path
}
