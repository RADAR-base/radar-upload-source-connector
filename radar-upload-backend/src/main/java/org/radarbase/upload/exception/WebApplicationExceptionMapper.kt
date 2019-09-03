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

package org.radarbase.upload.exception

import org.slf4j.LoggerFactory
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

/** Handle WebApplicationException. This uses the status code embedded in the exception. */
@Provider
class WebApplicationExceptionMapper : ExceptionMapper<WebApplicationException> {
    @Context
    private lateinit var uriInfo: UriInfo

    override fun toResponse(exception: WebApplicationException): Response {
        val response = exception.response
        logger.error("[{}] {}: {}", response.status, uriInfo.absolutePath, exception.message)
        return response
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WebApplicationExceptionMapper::class.java)
    }
}
