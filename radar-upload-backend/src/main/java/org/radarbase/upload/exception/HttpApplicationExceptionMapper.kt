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

import com.fasterxml.jackson.core.util.BufferRecyclers
import org.glassfish.jersey.message.internal.ReaderWriter
import org.slf4j.LoggerFactory
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

@Provider
class HttpApplicationExceptionMapper : ExceptionMapper<HttpApplicationException> {
    @Context
    private lateinit var uriInfo: UriInfo

    override fun toResponse(exception: HttpApplicationException): Response {
        logger.error("[{}] {} - {}: {}", exception.status, uriInfo.absolutePath, exception.code, exception.detailedMessage)

        val stringEncoder = BufferRecyclers.getJsonStringEncoder()
        val quotedError = stringEncoder.quoteAsUTF8(exception.code).toString(ReaderWriter.UTF8)
        val quotedDescription = stringEncoder.quoteAsUTF8(exception.detailedMessage).toString(ReaderWriter.UTF8)
        return Response.status(exception.status)
                .header("Content-Type", "application/json; charset=utf-8")
                .entity("{\"error\":\"$quotedError\","
                        + "\"error_description\":\"$quotedDescription\"}")
                .build()

    }

    companion object {
        private val logger = LoggerFactory.getLogger(HttpApplicationExceptionMapper::class.java)
    }
}
