package org.radarbase.upload.resource

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.radarbase.upload.auth.Auth
import org.radarbase.upload.auth.Authenticated
import org.radarbase.upload.auth.NeedsPermission
import org.radarbase.upload.doa.RecordRepository
import org.radarbase.upload.doa.SourceTypeRepository
import org.radarbase.upload.doa.entity.RecordStatus
import org.radarbase.upload.dto.*
import org.radarcns.auth.authorization.Permission
import org.radarcns.auth.authorization.Permission.*
import java.io.InputStream
import javax.annotation.Resource
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput
import javax.ws.rs.core.UriInfo
import javax.ws.rs.WebApplicationException


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

    @Context
    lateinit var sourceTypeRepository: SourceTypeRepository

    @Context
    lateinit var httpClient: OkHttpClient

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

    @POST
    @NeedsPermission(Entity.MEASUREMENT, Operation.CREATE)
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
    @NeedsPermission(Entity.MEASUREMENT, Operation.CREATE)
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
    @Path("{recordId}/contents/{fileName}")
    fun getContents(
            @PathParam("fileName") fileName: String,
            @PathParam("recordId") recordId: Long,
            @Context response: HttpServletResponse): StreamingOutput {

        val recordContent = recordRepository.readContent(recordId, fileName)
                ?: throw NotFoundException("Cannot find content with record-id $recordId and file-name $fileName")

        response.status = Response.Status.OK.statusCode
        response.setHeader("Content-type", recordContent.contentType)
        response.setHeader("Content-Length", recordContent.content.length().toString())
        response.setHeader("Last-Modified", recordContent.createdDate.toString())
        val inputStream = recordContent.content.binaryStream
        return StreamingOutput {
            inputStream.use { inStream -> inStream.copyTo(it) }
            it.flush()
        }
    }

    @POST
    @Path("poll")
    fun poll(pollDTO: PollDTO): RecordContainerDTO {

        if (auth.isClientCredentials) {
            val imposedLimit = Math.min(Math.max(pollDTO.limit, 1), 100)
            return recordMapper.fromRecords(recordRepository.poll(imposedLimit), imposedLimit)
        } else {
            throw NotAuthorizedException("Client is not authorized to poll records")
        }
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
    fun updateRecordMetaData(metaData: RecordMetadataDTO, @PathParam("recordId") recordId: Long, @Context callbackManager: CallbackManager): RecordMetadataDTO {
        val updatedRecord = recordRepository.updateMetadata(recordId, metaData)

        val updatedMetadata = recordMapper.fromMetadata(updatedRecord)

        callbackManager.callback(updatedMetadata)

        return updatedMetadata
    }

    @GET
    @Path("{recordId}/logs/")
    fun getRecordLogs(
            @PathParam("recordId") recordId: Long,
            @Context response: HttpServletResponse): StreamingOutput {

        val record = recordRepository.read(recordId)
                ?: throw NotFoundException("Record with ID $recordId does not exist")

        val charStream = record.metadata.logs?.logs?.characterStream
                ?: throw NotFoundException("Cannot find logs for record with record id $recordId")

        response.status = Response.Status.OK.statusCode
        response.setHeader("Content-type", "text/plain")
        response.setHeader("Last-Modified", record.metadata.modifiedDate.toString())

        return StreamingOutput {
            val writer = it.writer()
            charStream.use {
                reader -> reader.copyTo(writer)
            }
            writer.flush()
        }
    }
}
