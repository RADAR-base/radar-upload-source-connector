package org.radarbase.upload.resource

import org.radarbase.upload.auth.Auth
import org.radarbase.upload.auth.Authenticated
import org.radarbase.upload.auth.NeedsPermission
import org.radarbase.upload.doa.RecordRepository
import org.radarbase.upload.dto.RecordContainerDTO
import org.radarbase.upload.dto.RecordDTO
import org.radarbase.upload.dto.RecordMapper
import org.radarcns.auth.authorization.Permission
import org.radarcns.auth.authorization.Permission.PROJECT_READ
import org.radarcns.auth.authorization.Permission.SUBJECT_READ
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType

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
        if (record.id != null) {

        }

        // TODO: logic to do authorization checking
        // TODO: logic to do data checking

        // TODO: logic to add record to DB

        // TODO: logic to return DB object

        return record
    }


    // TODO: PUT path

    // TODO: POLL path

    // TODO: update metadata path

    // TODO: read file contents path

    // TODO: read log contents path
}
