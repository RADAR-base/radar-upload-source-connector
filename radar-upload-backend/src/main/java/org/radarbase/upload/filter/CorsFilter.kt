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

package org.radarbase.upload.filter

import org.radarbase.upload.Config
import java.io.IOException
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.container.ContainerResponseFilter
import javax.ws.rs.core.Context
import javax.ws.rs.ext.Provider


@Provider
class CorsFilter: ContainerResponseFilter {

    @Context
    private lateinit var config: Config

    @Throws(IOException::class)
    override fun filter(request: ContainerRequestContext,
               response: ContainerResponseContext) {
        if (config.enableCors!!) {
            response.headers.add("Access-Control-Allow-Origin",
                    request.getHeaderString("Origin") ?: "*")
            response.headers.add("Access-Control-Allow-Headers",
                    "origin, content-type, accept, authorization")
            response.headers.add("Access-Control-Allow-Credentials", "true")
            response.headers.add("Access-Control-Allow-Methods",
                    "GET, POST, PUT, DELETE, OPTIONS, HEAD")
        }
    }
}
