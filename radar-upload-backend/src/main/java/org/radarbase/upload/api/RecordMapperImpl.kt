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

package org.radarbase.upload.api

import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.UriInfo
import org.radarbase.jersey.service.AsyncCoroutineService
import org.radarbase.jersey.service.managementportal.RadarProjectService
import org.radarbase.upload.Config
import org.radarbase.upload.doa.SourceTypeRepository
import org.radarbase.upload.doa.entity.Record
import org.radarbase.upload.doa.entity.RecordContent
import org.radarbase.upload.doa.entity.RecordMetadata
import org.radarbase.upload.doa.entity.RecordStatus
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.time.Instant

class RecordMapperImpl(
    @Context private val uri: UriInfo,
    @Context private val sourceTypeRepository: SourceTypeRepository,
    @Context private val projectService: RadarProjectService,
    @Context private val config: Config,
    @Context private val asyncService: AsyncCoroutineService,
) : RecordMapper {
    override suspend fun cleanBaseUri(): String {
        val baseUrl = config.advertisedBaseUri ?: asyncService.runInRequestScope { uri.baseUri }
        return baseUrl.toString().trimEnd('/')
    }

    override suspend fun toRecord(record: RecordDTO): Pair<Record, RecordMetadata> {
        val recordDoa = Record().apply {
            val data = record.data ?: throw BadRequestException("No data field included")
            projectId = data.projectId ?: throw BadRequestException("Missing project ID")
            userId = data.resolveUserId()
            sourceId = data.sourceId ?: throw BadRequestException("Missing source ID")
            sourceType = sourceTypeRepository.read(
                record.sourceType
                    ?: throw BadRequestException("Missing source type"),
            )
                ?: throw BadRequestException("Source type not found")
        }

        return Pair(recordDoa, toMetadata(record.metadata))
    }

    private suspend fun RecordDataDTO.resolveUserId(): String {
        val userId = userId
        return if (!userId.isNullOrEmpty()) {
            userId
        } else {
            val externalUserId = externalUserId?.takeIf { it.isNotEmpty() }
                ?: throw BadRequestException("Missing both user ID and external-user ID or they are empty")
            val projectId = projectId
                ?: throw BadRequestException("Missing project ID")

            logger.info("Fetching user id by externalId {}", externalUserId)
            val user = projectService.subjectByExternalId(projectId, externalUserId)
            user ?: throw BadRequestException("Cannot find a user with externalID ${this.externalUserId}")
            return user.id ?: throw BadRequestException("Cannot resolve user id")
        }
    }

    private fun toMetadata(metadata: RecordMetadataDTO?) = RecordMetadata().apply {
        createdDate = Instant.now()
        modifiedDate = Instant.now()
        revision = 1

        status = metadata?.status?.let {
            try {
                RecordStatus.valueOf(it)
            } catch (ex: IllegalArgumentException) {
                null
            }
        } ?: RecordStatus.INCOMPLETE

        callbackUrl = metadata?.callbackUrl
    }

    override suspend fun fromRecord(record: Record) = RecordDTO(
        id = record.id!!,
        metadata = fromMetadata(record.metadata).apply { id = null },
        sourceType = record.sourceType.name,
        data = RecordDataDTO(
            projectId = record.projectId,
            userId = record.userId,
            sourceId = record.sourceId,
            time = record.time,
            timeZoneOffset = record.timeZoneOffset,
            contents = record.contents?.mapTo(HashSet()) { fromContent(it) },
        ),
    )

    override suspend fun fromRecords(records: List<Record>, page: Page?) = RecordContainerDTO(
        records = records.map { fromRecord(it) },
        size = page?.pageSize,
        totalElements = page?.totalElements,
        page = page?.pageNumber,
    )

    override suspend fun fromContent(content: RecordContent): ContentsDTO {
        @Suppress("BlockingMethodInNonBlockingContext")
        val cleanedFileName = URLEncoder.encode(content.fileName, "UTF-8")
            .replace("+", "%20")

        return ContentsDTO(
            url = "${cleanBaseUri()}/records/${content.record.id}/contents/$cleanedFileName",
            contentType = content.contentType,
            createdDate = content.createdDate,
            size = content.size,
            fileName = content.fileName,
        )
    }

    override suspend fun fromMetadata(metadata: RecordMetadata) = RecordMetadataDTO(
        id = metadata.id,
        revision = metadata.revision,
        status = metadata.status.name,
        message = metadata.message,
        createdDate = metadata.createdDate,
        modifiedDate = metadata.modifiedDate,
        committedDate = metadata.committedDate,
        // use record.id, since metadata and record have one-to-one
        logs = metadata.logs?.let {
            LogsDto(url = "${cleanBaseUri()}/records/${metadata.id}/logs")
        },
    )

    companion object {
        private val logger = LoggerFactory.getLogger(RecordMapperImpl::class.java)
    }
}
