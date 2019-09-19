/*
 *
 *  * Copyright 2019 The Hyve
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */

package org.radarbase.upload.resource

import com.fasterxml.jackson.databind.ObjectMapper
import org.radarbase.upload.api.*
import org.radarbase.upload.auth.Auth
import org.radarbase.upload.auth.Authenticated
import org.radarbase.upload.auth.NeedsPermission
import org.radarbase.upload.doa.RecordRepository
import org.radarbase.upload.doa.SourceTypeRepository
import org.radarbase.upload.doa.entity.Record
import org.radarbase.upload.doa.entity.RecordStatus
import org.radarbase.upload.dto.CallbackManager
import org.radarbase.upload.exception.ConflictException
import org.radarcns.auth.authorization.Permission
import org.radarcns.auth.authorization.Permission.*
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.net.URI
import javax.annotation.Resource
import javax.ws.rs.*
import javax.ws.rs.core.*
import kotlin.math.max
import kotlin.math.min
import org.radarbase.upload.exception.BadRequestException as RbBadRequestException
import org.radarbase.upload.exception.NotFoundException as RbNotFoundException

@Path("records")
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

    @GET
    fun query(
            @QueryParam("projectId") projectId: String?,
            @QueryParam("userId") userId: String?,
            @DefaultValue("10") @QueryParam("limit") limit: Int,
            @QueryParam("lastId") lastId: Long?,
            @QueryParam("sourceType") sourceType: String?,
            @QueryParam("status") status: String?): RecordContainerDTO {
        projectId ?: throw RbBadRequestException("missing_project", "Required project ID not provided.")

        if (userId != null) {
            auth.checkUserPermission(SUBJECT_READ, projectId, userId)
        } else {
            auth.checkProjectPermission(PROJECT_READ, projectId)
        }

        val imposedLimit = min(max(limit, 1), 100)
        val records = recordRepository.query(imposedLimit, lastId ?: -1L, projectId, userId, status, sourceType)

        return recordMapper.fromRecords(records, imposedLimit)
    }

    @POST
    @NeedsPermission(Entity.MEASUREMENT, Operation.CREATE)
    fun create(record: RecordDTO): Response {
        validateNewRecord(record, auth)

        val (doaRecord, metadata) = recordMapper.toRecord(record)
        val result = recordRepository.create(doaRecord, metadata, record.data?.contents)

        logger.info("Record created $result")
        return Response.created(URI("${uri.baseUri}records/${result.id}"))
                .entity(recordMapper.fromRecord(result))
                .build()
    }

    @DELETE
    @Path("{recordId}")
    @NeedsPermission(Entity.MEASUREMENT, Operation.CREATE)
    fun delete(@PathParam("recordId") recordId: Long,
            @QueryParam("revision") revision: Int): Response {
        val record = ensureRecord(recordId)

        recordRepository.delete(record, revision)

        return Response.noContent().build()
    }

    @GET
    @Path("{recordId}")
    @NeedsPermission(Entity.SUBJECT, Operation.READ)
    fun readRecord(@PathParam("recordId") recordId: Long): RecordDTO {
        val record = ensureRecord(recordId)

        return recordMapper.fromRecord(record)
    }

    @DELETE
    @Path("{recordId}/contents/{fileName}")
    fun deleteContents(
            @PathParam("recordId") recordId: Long,
            @PathParam("fileName") fileName: String): Response {
        val record = ensureRecord(recordId)

        recordRepository.deleteContents(record, fileName)

        return Response.noContent().build()
    }

    private fun validateNewRecord(record: RecordDTO, auth: Auth) {
        if (record.id != null) {
            throw RbBadRequestException("field_forbidden", "Record ID cannot be set explicitly")
        }
        val sourceTypeName = record.sourceType
                ?: throw RbBadRequestException("field_missing", "Record needs a source type")
        val data = record.data ?: throw RbBadRequestException("field_missing", "Record needs data")

        data.projectId ?: throw RbBadRequestException("project_missing", "Record needs a project ID")
        data.userId ?: throw RbBadRequestException("user_missing", "Record needs a user ID")

        auth.checkUserPermission(MEASUREMENT_CREATE, data.projectId, data.userId)

        data.contents?.forEach {
            it.text ?: throw RbBadRequestException("field_missing", "Contents need explicit text value set in UTF-8 encoding.")
            if (it.url != null) {
                throw RbBadRequestException("field_forbidden", "Cannot process URL for content file name ${it.fileName}")
            }
        }
        if (record.metadata != null) {
            throw RbBadRequestException("field_forbidden", "Record metadata cannot be set explicitly")
        }

        val sourceType = sourceTypeRepository.read(sourceTypeName)
                ?: throw RbBadRequestException("source_type_not_found", "Source type $sourceTypeName does not exist.")

        if (sourceType.timeRequired && (data.time == null || data.timeZoneOffset == null)) {
            throw RbBadRequestException("field_missing", "Time and time zone offset values are required for this source type.")
        }
        if (sourceType.sourceIdRequired && data.sourceId == null) {
            throw RbBadRequestException("field_missing", "Source ID is required for source type $sourceTypeName.")
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

        val record = ensureRecord(recordId)

        if (record.metadata.status != RecordStatus.INCOMPLETE) {
            throw ConflictException("incompatible_status", "Cannot add files to saved record.")
        }

        val content = recordRepository.updateContent(record, fileName, contentType, input, contentLength)

        val contentDto = recordMapper.fromContent(content)

        return Response.created(URI(contentDto.url!!))
                .entity(contentDto)
                .build()
    }

    private fun ensureRecord(recordId: Long, permission: Permission? = MEASUREMENT_CREATE): Record {
        val record = recordRepository.read(recordId)
                ?: throw RbNotFoundException("record_not_found", "Record with ID $recordId does not exist")

        permission?.let {
            auth.checkUserPermission(it, record.projectId, record.userId)
        }

        return record
    }

    @GET
    @Path("{recordId}/contents/{fileName}")
    fun getContents(
            @PathParam("fileName") fileName: String,
            @PathParam("recordId") recordId: Long): Response {

        val record = ensureRecord(recordId)

        val recordContent = record.contents?.find { it.fileName == fileName }
                ?: throw RbNotFoundException("file_not_found", "Cannot read content of file $fileName from record $recordId")

        logger.debug("Reading record $recordId file $fileName of size ${recordContent.size}")

        val streamingOutput = StreamingOutput { out ->
            try {
                recordRepository.readFileContent(recordId, record.metadata.revision, recordContent.fileName).use {
                    it?.stream?.copyTo(out)
                            ?: throw RbNotFoundException("file_not_found", "Cannot read content of file $fileName from record $recordId")
                }
                out.flush()
            } catch (ex: IOException) {
                logger.error("Failed to respond with file contents: {}. Caused by?: {}",
                        ex.toString(), ex.cause?.toString())
            }
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
            val imposedLimit = pollDTO.limit
                    .coerceAtLeast(1)
                    .coerceAtMost(100)
            val records = recordRepository.poll(imposedLimit, pollDTO.supportedConverters)
            return recordMapper.fromRecords(records, imposedLimit)
        } else {
            throw NotAuthorizedException("Client is not authorized to poll records")
        }
    }

    @GET
    @Path("{recordId}/metadata")
    fun getRecordMetaData(@PathParam("recordId") recordId: Long): RecordMetadataDTO {
        val record = ensureRecord(recordId, SUBJECT_READ)

        return recordMapper.fromMetadata(record.metadata)
    }

    @POST
    @Path("{recordId}/metadata")
    fun updateRecordMetaData(
            metaData: RecordMetadataDTO,
            @PathParam("recordId") recordId: Long,
            @Context callbackManager: CallbackManager): RecordMetadataDTO {
        ensureRecord(recordId)

        val updatedRecord = recordRepository.updateMetadata(recordId, metaData)

        return recordMapper.fromMetadata(updatedRecord)
                .also { callbackManager.callback(it) }
    }

    @GET
    @Path("{recordId}/logs")
    fun getRecordLogs(
            @PathParam("recordId") recordId: Long): Response {
        val record = ensureRecord(recordId, SUBJECT_READ)
        recordRepository.readLogs(recordId)
                ?: throw RbNotFoundException("log_not_found", "Cannot find logs for record with record id $recordId")

        val streamingOutput = StreamingOutput { out ->
            try {
                recordRepository.readLogContents(recordId).use { clobReader ->
                    clobReader
                            ?: throw RbNotFoundException("log_not_found", "Cannot find logs for record with record id $recordId")

                    clobReader.stream.use { reader ->
                        val buffer = CharArray(LOG_BUFFER_SIZE)

                        generateSequence { reader.read(buffer) }
                                .takeWhile { n -> n != -1 }
                                .forEach { n -> out.write(String(buffer, 0, n).toByteArray()) }
                    }
                }
                out.flush()
            } catch (ex: IOException) {
                logger.error("Failed to respond with log contents: {}. Caused by?: {}",
                        ex.toString(), ex.cause?.toString())
            }
        }

        return Response.ok(streamingOutput, "text/plain")
                .header("Last-Modified", record.metadata.modifiedDate)
                .build()
    }

    @PUT
    @Consumes("text/plain")
    @Path("{recordId}/logs")
    fun addRecordLogs(
            recordLogs: String,
            @PathParam("recordId") recordId: Long): RecordMetadataDTO {
        ensureRecord(recordId, SUBJECT_UPDATE)

        val uploadedMetaData = recordRepository.updateLogs(recordId, recordLogs)
        return recordMapper.fromMetadata(uploadedMetaData)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RecordResource::class.java)

        private const val LOG_BUFFER_SIZE = 65536
    }
}
