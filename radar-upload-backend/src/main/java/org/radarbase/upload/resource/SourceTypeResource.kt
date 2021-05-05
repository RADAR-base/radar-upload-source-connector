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

import org.radarbase.jersey.cache.Cache
import org.radarbase.upload.api.SourceTypeContainerDTO
import org.radarbase.upload.api.SourceTypeDTO
import org.radarbase.upload.api.SourceTypeMapper
import org.radarbase.upload.doa.SourceTypeRepository
import jakarta.annotation.Resource
import jakarta.inject.Singleton
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType

@Path("source-types")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Resource
@Singleton
class SourceTypeResource(
    @Context private var sourceTypeRepository: SourceTypeRepository,
    @Context private var sourceTypeMapper: SourceTypeMapper,
) {
    @GET
    @Cache(maxAge = 300)
    fun query(
        @DefaultValue("20") @QueryParam("limit") limit: Int,
        @QueryParam("lastId") lastId: Long?,
    ): SourceTypeContainerDTO {
        val imposedLimit = limit
            .coerceAtLeast(1)
            .coerceAtMost(100)

        val records = sourceTypeRepository.readAll(imposedLimit, lastId)

        return sourceTypeMapper.fromSourceTypes(records)
    }

    @GET
    @Cache(maxAge = 3600)
    @Path("{name}")
    fun getSourceType(
            @PathParam("name") name: String,
    ): SourceTypeDTO {

        val record = sourceTypeRepository.read(name)
            ?: throw NotFoundException("Source type with name $name not found")

        return sourceTypeMapper.fromSourceType(record)
    }
}
