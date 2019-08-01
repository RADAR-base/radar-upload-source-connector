package org.radarbase.upload.resource

import com.fasterxml.jackson.databind.ObjectMapper
import org.radarbase.upload.api.*
import org.radarbase.upload.auth.Auth
import org.radarbase.upload.auth.Authenticated
import org.radarbase.upload.auth.NeedsPermission
import org.radarbase.upload.doa.RecordRepository
import org.radarbase.upload.doa.SourceTypeRepository
import org.radarbase.upload.doa.entity.RecordStatus
import org.radarbase.upload.dto.CallbackManager
import org.radarcns.auth.authorization.Permission.*
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.lang.Exception
import java.lang.IllegalStateException
import java.net.URI
import javax.annotation.Resource
import javax.persistence.EntityManager
import javax.ws.rs.*
import javax.ws.rs.core.*


@Path("/records")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Resource
@Authenticated
class RecordResource {

    @Context
    lateinit var recordRepository: RecordRepository

    @Context
    lateinit var mapper: ObjectMapper

    @Context
    lateinit var recordMapper: RecordMapper

    @Context
    lateinit var uri: UriInfo

    @Context
    lateinit var auth: Auth

    @Context
    lateinit var sourceTypeRepository: SourceTypeRepository

    @Context
    lateinit var em: EntityManager

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
    fun create(record: RecordDTO): Response {

        validateNewRecord(record, auth)

        val doaRecord = recordMapper.toRecord(record)
        val result = recordRepository.create(doaRecord)

        logger.info("Record created $result")
        return Response.created(URI("${uri.baseUri}records/${result.id}"))
                .entity(recordMapper.fromRecord(result))
                .build()
    }

    private fun validateNewRecord(record: RecordDTO, auth: Auth) {
        if (record.id != null) {
            throw BadRequestException("Record ID cannot be set explicitly")
        }
        val sourceTypeName = record.sourceType
                ?: throw BadRequestException("Record needs a source type")
        val data = record.data ?: throw BadRequestException("Record needs data")

        data.projectId ?: throw BadRequestException("Record needs a project ID")
        data.userId ?: throw BadRequestException("Record needs a user ID")

        auth.checkUserPermission(MEASUREMENT_CREATE, data.projectId, data.userId)

        data.contents?.forEach {
            it.text
                    ?: throw BadRequestException("Contents need explicit text value set in UTF-8 encoding.")
            if (it.url != null) {
                throw BadRequestException("Cannot process URL for content file name ${it.fileName}")
            }
        }
        if (record.metadata != null) {
            throw BadRequestException("Record metadata cannot be set explicitly")
        }

        val sourceType = sourceTypeRepository.read(sourceTypeName)
                ?: throw BadRequestException("Source type $sourceTypeName does not exist.")

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
            @PathParam("recordId") recordId: Long): Response {

        val record = recordRepository.read(recordId)
                ?: throw NotFoundException("Record with ID $recordId does not exist")

        auth.checkUserPermission(MEASUREMENT_CREATE, record.projectId, record.userId)

        if (record.metadata.status != RecordStatus.INCOMPLETE) {
            throw WebApplicationException("Cannot add files to saved record.", Response.Status.CONFLICT)
        }

        val content = recordRepository.updateContent(record, fileName, contentType, input, contentLength)

        val contentDto = recordMapper.fromContent(content)

        return Response.created(URI(contentDto.url!!))
                .entity(contentDto)
                .build()
    }

    @GET
    @Path("{recordId}/contents/{fileName}")
    fun getContents(
            @PathParam("fileName") fileName: String,
            @PathParam("recordId") recordId: Long): Response {

        val recordContent = recordRepository.readRecordContent(recordId, fileName)
                ?: throw NotFoundException("Cannot find record content with record-id $recordId and fileName $fileName")

        val content = recordRepository.readFileContent(recordId, fileName)
                ?: throw NotFoundException("Cannot read content of file $fileName from record $recordId")

        val streamingOutput = StreamingOutput {
            content.inputStream().use { inStream -> inStream.copyTo(it) }
            it.flush()
        }

        return Response
                .ok(streamingOutput)
                .header("Content-type", recordContent.contentType)
                .header("Content-Length", recordContent.size)
                .header("Last-Modified", recordContent.createdDate)
                .build()

    }

    @POST
    @Path("poll")
    fun poll(pollDTO: PollDTO): RecordContainerDTO {
        if (auth.isClientCredentials) {
            val imposedLimit = Math.min(Math.max(pollDTO.limit, 1), 100)
            val records = recordRepository.poll(imposedLimit)
            return recordMapper.fromRecords(records, imposedLimit)
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
    fun updateRecordMetaData(
            metaData: RecordMetadataDTO,
            @PathParam("recordId") recordId: Long,
            @Context callbackManager: CallbackManager): RecordMetadataDTO {
        val updatedRecord = recordRepository.updateMetadata(recordId, metaData)

        return recordMapper.fromMetadata(updatedRecord)
                .also { callbackManager.callback(it) }
    }

    @GET
    @Path("{recordId}/logs")
    fun getRecordLogs(
            @PathParam("recordId") recordId: Long): Response {

        val record = recordRepository.read(recordId)
                ?: throw NotFoundException("Record with ID $recordId does not exist")

        val charStream = record.metadata.logs?.logs?.characterStream
                ?: throw NotFoundException("Cannot find logs for record with record id $recordId")

        val streamingOutput = StreamingOutput {
            val writer = it.writer()
            charStream.use { reader ->
                reader.copyTo(writer)
            }
            writer.flush()

        }
        return Response.ok(streamingOutput, "text/plain")
                .header("Last-Modified", record.metadata.modifiedDate)
                .build()
    }

    @POST
    @Path("{recordId}/logs")
    fun addRecordLogs(
            recordLogs: LogsDto,
            @PathParam("recordId") recordId: Long): RecordMetadataDTO {
        recordLogs.contents
                ?: throw IllegalStateException("No content provided to update the logs")

        val uploadedMetaData = recordRepository.updateLogs(recordId, recordLogs.contents!!)
        return recordMapper.fromMetadata(uploadedMetaData)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RecordResource::class.java)
    }
}
