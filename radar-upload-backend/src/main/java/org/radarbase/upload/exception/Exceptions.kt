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

import java.lang.RuntimeException
import javax.ws.rs.core.Response.Status

open class HttpApplicationException(val status: Int, val code: String, val detailedMessage: String?) : RuntimeException("[$status] $code: $detailedMessage") {
    constructor(status: Status, code: String, detailedMessage: String?) : this(status.statusCode, code, detailedMessage)
}

class BadGatewayException(message: String) :
        HttpApplicationException(Status.BAD_GATEWAY, "bad_gateway", message)

class BadRequestException(code: String, message: String) :
        HttpApplicationException(Status.BAD_REQUEST, code, message)

class ConflictException(code: String, messageText: String) :
        HttpApplicationException(Status.CONFLICT, code, messageText)

class NotAuthorizedException(code: String, message: String)
    : HttpApplicationException(Status.UNAUTHORIZED, code, message)

class NotFoundException(code: String, message: String) :
        HttpApplicationException(Status.NOT_FOUND, code, message)

class InternalServerException(code: String, message: String) :
        HttpApplicationException(Status.INTERNAL_SERVER_ERROR, code, message)
