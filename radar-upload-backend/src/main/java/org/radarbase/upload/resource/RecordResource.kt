package org.radarbase.upload.resource

import org.radarbase.upload.auth.Auth
import org.radarbase.upload.auth.Authenticated
import org.radarbase.upload.auth.NeedsPermission
import org.radarbase.upload.doa.RecordRepository
import org.radarbase.upload.doa.SourceTypeRepository
import org.radarbase.upload.doa.entity.RecordMetadata
import org.radarbase.upload.doa.entity.RecordStatus
import org.radarbase.upload.dto.*
import org.radarbase.upload.exception.ConflictException
import org.radarcns.auth.authorization.Permission
import org.radarcns.auth.authorization.Permission.*
import java.io.InputStream
import javax.annotation.Resource
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo


@Path("/records")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Resource
@Authenticated
class RecordResource {

    @Context
    lateinit var recordRepository: RecordRepository

    @Context
    lateinit var auth: Auth

    @Context
    lateinit var recordMapper: RecordMapper

    @Context
    lateinit var uri: UriInfo

    @GET
    fun query(
            @QueryParam("projectId") projectId: String?,
            @QueryParam("userId") userId: String?,
            @DefaultValue("10") @QueryParam("limit") limit: Int,
            @QueryParam("lastId") lastId: Long?,
            @QueryParam("status") status: String?): RecordContainerDTO {

        projectId ?: throw BadRequestException("Required project ID not provided.")

        if (userId != null) {
            auth.checkUserPermission(SUBJECT_READ, projectId, userId)
        } else {
            auth.checkProjectPermission(PROJECT_READ, projectId)
        }

        val imposedLimit = Math.min(Math.max(limit, 1), 100)
        val records = recordRepository.query(imposedLimit, lastId ?: -1L, projectId, userId, status)

        return recordMapper.fromRecords(records, imposedLimit)
    }

    @Context
    lateinit var sourceTypeRepository: SourceTypeRepository

    @POST
    @NeedsPermission(Permission.Entity.MEASUREMENT, Permission.Operation.CREATE)
    fun create(record: RecordDTO, @Context response: HttpServletResponse): RecordDTO {

        // TODO: logic to do authorization checking

        validateNewRecord(record)

        val doaRecord = recordMapper.toRecord(record)
        val result = recordRepository.create(doaRecord)

        response.status = Response.Status.CREATED.statusCode
        response.setHeader("Location", "${uri.baseUri}/records/${record.id}")
        return recordMapper.fromRecord(result)
    }

    private fun validateNewRecord(record: RecordDTO) {
        if (record.id != null) {
            throw BadRequestException("Record ID cannot be set explicitly")
        }
        val sourceTypeName = record.sourceType ?: throw BadRequestException("Record needs a source type")
        val data = record.data ?: throw BadRequestException("Record needs data")

        data.projectId ?: throw BadRequestException("Record needs a project ID")
        data.userId ?: throw BadRequestException("Record needs a user ID")

        data.contents?.forEach {
            it.text ?: throw BadRequestException("Contents need explicit text value set in UTF-8 encoding.")
            if (it.url != null) {
                throw BadRequestException("Cannot process URL for content file name ${it.fileName}")
            }
        }
        if (record.metadata != null) {
            throw BadRequestException("Record metadata cannot be set explicitly")
        }

        val sourceType = sourceTypeRepository.read(sourceTypeName) ?: throw BadRequestException("Source type $sourceTypeName does not exist.")

        if (sourceType.timeRequired && (data.time == null || data.timeZoneOffset == null)) {
            throw BadRequestException("Time and time zone offset values are required for this source type.")
        }
        if (sourceType.sourceIdRequired && data.sourceId == null) {
            throw BadRequestException("Source ID is required for source type $sourceTypeName.")
        }
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
            @PathParam("recordId") recordId: Long,
            @Context response: HttpServletResponse): ContentsDTO {

        val record = recordRepository.read(recordId)
                ?: throw NotFoundException("Record with ID $recordId does not exist")

        auth.checkUserPermission(MEASUREMENT_CREATE, record.projectId, record.userId)

        if (record.metadata.status != RecordStatus.INCOMPLETE) {
            throw WebApplicationException("Cannot add files to saved record.", Response.Status.CONFLICT)
        }

        val content = recordRepository.updateContent(record, fileName, contentType, input, contentLength)

        val contentDto = recordMapper.fromContent(content)
        response.status = Response.Status.CREATED.statusCode
        response.setHeader("Location", contentDto.url)
        return contentDto
    }

    @GET
    @Path("poll")
    fun poll(@DefaultValue("10") @QueryParam("limit") limit: Int): RecordContainerDTO {
        // TODO: only allow with client credentials

        val imposedLimit = Math.min(Math.max(limit, 1), 100)

        return recordMapper.fromRecords(recordRepository.poll(imposedLimit), imposedLimit)
    }

    @GET
    @Path("{recordId}/metadata")
    fun getRecordMetaData(@PathParam("recordId") recordId: Long): RecordMetadataDTO {

        val record = recordRepository.read(recordId)
                ?: throw NotFoundException("Record with ID $recordId does not exist")

        return recordMapper.fromMetadata(record.metadata)
    }

    @POST
    @Path("{recordId}/metadata")
    fun updateRecordMetaData(metaData: RecordMetadataDTO, @PathParam("recordId") recordId: Long): RecordMetadataDTO {

        val record = recordRepository.read(recordId)
                ?: throw NotFoundException("Record with ID $recordId does not exist")

        record.metadata.canBeUpdatedTo(metaData)

        val updatedRecord = recordRepository.update(recordMapper.toMetadata(metaData, record.metadata))

        return recordMapper.fromMetadata(updatedRecord)
    }

    private fun RecordMetadata.canBeUpdatedTo(metaDataToUpdate: RecordMetadataDTO) {

        if (this.revision != metaDataToUpdate.revision)
            throw BadRequestException("Requested meta data revision ${metaDataToUpdate.revision} " +
                    "should match the latest revision stored ${this.revision}")

        if (metaDataToUpdate.status == RecordStatus.PROCESSING.toString()
                && this.status != RecordStatus.QUEUED) {
            throw ConflictException("Record cannot be updated: Conflict in record meta-data status. " +
                    "Found ${this.status}, expected ${RecordStatus.QUEUED}")
        }

        if (metaDataToUpdate.status == RecordStatus.SUCCEEDED.toString()
                || metaDataToUpdate.status == RecordStatus.FAILED.toString()
                && this.status != RecordStatus.PROCESSING) {
            throw ConflictException("Record cannot be updated: Conflict in record meta-data status. " +
                    "Found ${this.status}, expected ${RecordStatus.QUEUED}")
        }

    }


    // TODO: read file contents path

    // TODO: read log contents path
}
