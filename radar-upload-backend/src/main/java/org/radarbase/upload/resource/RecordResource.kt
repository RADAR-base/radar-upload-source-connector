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

import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.auth.Authenticated
import org.radarbase.jersey.auth.NeedsPermission
import org.radarbase.jersey.cache.Cache
import org.radarbase.jersey.exception.HttpBadRequestException
import org.radarbase.jersey.exception.HttpConflictException
import org.radarbase.jersey.exception.HttpNotFoundException
import org.radarbase.jersey.service.managementportal.RadarProjectService
import org.radarbase.upload.api.*
import org.radarbase.upload.doa.RecordRepository
import org.radarbase.upload.doa.SourceTypeRepository
import org.radarbase.upload.doa.entity.Record
import org.radarbase.upload.doa.entity.RecordStatus
import org.radarbase.upload.dto.CallbackManager
import org.radarbase.auth.authorization.Permission
import org.radarbase.auth.authorization.Permission.*
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.net.URI
import jakarta.annotation.Resource
import jakarta.inject.Singleton
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.StreamingOutput

@Path("records")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Resource
@Authenticated
@Singleton
class RecordResource(
        @Context private val recordRepository: RecordRepository,
        @Context private val recordMapper: RecordMapper,
        @Context private val auth: Auth,
        @Context private val sourceTypeRepository: SourceTypeRepository,
        @Context private val projectService: RadarProjectService,
) {

    @GET
    fun query(
            @QueryParam("projectId") projectId: String?,
            @QueryParam("userId") userId: String?,
            @QueryParam("size") pageSize: Int?,
            @DefaultValue("1") @QueryParam("page") pageNumber: Int,
            @QueryParam("sourceType") sourceType: String?,
            @QueryParam("status") status: String?): RecordContainerDTO {
        projectId ?: throw HttpBadRequestException("missing_project", "Required project ID not provided.")

        if (userId != null) {
            auth.checkPermissionOnSubject(SUBJECT_READ, projectId, userId)
        } else {
            auth.checkPermissionOnProject(PROJECT_READ, projectId)
        }

        val queryPage = Page(pageNumber = pageNumber, pageSize = pageSize)
        val (records, page) = recordRepository.query(queryPage, projectId, userId, status, sourceType)

        return recordMapper.fromRecords(records, page)
    }

    @POST
    @NeedsPermission(Entity.MEASUREMENT, Operation.CREATE)
    fun create(record: RecordDTO): Response {
        validateNewRecord(record)

        val (doaRecord, metadata) = recordMapper.toRecord(record)
        val result = recordRepository.create(doaRecord, metadata, record.data?.contents)

        logger.info("Record created $result")
        return Response.created(URI("${recordMapper.cleanBaseUri}/records/${result.id}"))
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

    private fun validateNewRecord(record: RecordDTO) {
        if (record.id != null) {
            throw HttpBadRequestException("field_forbidden", "Record ID cannot be set explicitly")
        }
        val sourceTypeName = record.sourceType
                ?: throw HttpBadRequestException("field_missing", "Record needs a source type")
        val data = record.data ?: throw HttpBadRequestException("field_missing", "Record needs data")

        val projectId = data.projectId
        if (projectId.isNullOrEmpty()) throw HttpBadRequestException("project_missing", "Record needs a project ID")
        if (data.userId.isNullOrEmpty() && data.externalUserId.isNullOrEmpty())
            throw HttpBadRequestException("user_missing", "Record needs a user ID or externalUserId")

        projectService.ensureProject(projectId)
        auth.checkPermissionOnSubject(MEASUREMENT_CREATE, projectId, data.userId)

        data.contents?.forEach {
            it.text ?: throw HttpBadRequestException("field_missing", "Contents need explicit text value set in UTF-8 encoding.")
            if (it.url != null) {
                throw HttpBadRequestException("field_forbidden", "Cannot process URL for content file name ${it.fileName}")
            }
        }
        if (record.metadata != null) {
            throw HttpBadRequestException("field_forbidden", "Record metadata cannot be set explicitly")
        }

        val sourceType = sourceTypeRepository.read(sourceTypeName)
                ?: throw HttpBadRequestException("source_type_not_found", "Source type $sourceTypeName does not exist.")

        if (sourceType.timeRequired && (data.time == null || data.timeZoneOffset == null)) {
            throw HttpBadRequestException("field_missing", "Time and time zone offset values are required for this source type.")
        }
        if (sourceType.sourceIdRequired && data.sourceId == null) {
            throw HttpBadRequestException("field_missing", "Source ID is required for source type $sourceTypeName.")
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
            throw HttpConflictException("incompatible_status", "Cannot add files to saved record.")
        }

        if (contentLength == 0L) {
            throw HttpBadRequestException("content-length-not-specified", "Content-Length header not specified in the request or invalid value found")
        }

        if (contentType.isEmpty()) {
            throw HttpBadRequestException("content-type-not-specified", "Content-Type header not specified in the request or invalid value found")
        }

        val content = recordRepository.updateContent(record, fileName, contentType, input, contentLength)

        val contentDto = recordMapper.fromContent(content)

        return Response.created(URI(contentDto.url!!))
                .entity(contentDto)
                .build()
    }

    private fun ensureRecord(recordId: Long, permission: Permission? = MEASUREMENT_CREATE): Record {
        val record = recordRepository.read(recordId)
                ?: throw HttpNotFoundException("record_not_found", "Record with ID $recordId does not exist")

        if (permission != null) {
            auth.checkPermissionOnSubject(permission, record.projectId, record.userId)
        }

        return record
    }

    @GET
    @Cache(maxAge = 86400, isPrivate = true)
    @Path("{recordId}/contents/{fileName}")
    fun getContents(
            @PathParam("fileName") fileName: String,
            @PathParam("recordId") recordId: Long): Response {

        val record = ensureRecord(recordId)

        val recordContent = record.contents?.find { it.fileName == fileName }
                ?: throw HttpNotFoundException("file_not_found", "Cannot read content of file $fileName from record $recordId")

        logger.debug("Reading record $recordId file $fileName of size ${recordContent.size}")

        val streamingOutput = StreamingOutput { out ->
            try {
                recordRepository.readFileContent(recordId, record.metadata.revision, recordContent.fileName).use {
                    it?.stream?.copyTo(out)
                            ?: throw HttpNotFoundException("file_not_found", "Cannot read content of file $fileName from record $recordId")
                }
                out.flush()
            } catch (ex: IOException) {
                logger.error("Failed to respond with file contents: {}. Caused by?: {}",
                        ex.toString(), ex.cause?.toString())
            }
        }

        return Response
                .ok(streamingOutput)
                .header("Content-Type", recordContent.contentType)
                .header("Content-Length", recordContent.size)
                .header("Last-Modified", recordContent.createdDate)
                .build()
    }

    @POST
    @Path("poll")
    fun poll(pollDTO: PollDTO): RecordContainerDTO {
        if (auth.token.grantType.equals("client_credentials", ignoreCase = true)) {
            val imposedLimit = pollDTO.limit
                    .coerceAtLeast(1)
                    .coerceAtMost(100)
            val records = recordRepository.poll(imposedLimit, pollDTO.supportedConverters)
            return recordMapper.fromRecords(records, page = Page(pageSize = imposedLimit))
        } else {
            throw NotAuthorizedException("Only for internal use")
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
                ?: throw HttpNotFoundException("log_not_found", "Cannot find logs for record with record id $recordId")

        val streamingOutput = StreamingOutput { out ->
            try {
                recordRepository.readLogContents(recordId).use { clobReader ->
                    clobReader
                            ?: throw HttpNotFoundException("log_not_found", "Cannot find logs for record with record id $recordId")

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
