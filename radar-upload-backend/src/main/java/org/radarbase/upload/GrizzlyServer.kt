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

package org.radarbase.upload

import org.glassfish.grizzly.http.server.HttpServer
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory
import org.radarbase.upload.inject.UploadResourceConfig
import java.util.concurrent.TimeUnit

class GrizzlyServer(private val config: Config) {
    private lateinit var httpServer: HttpServer

    fun start() {
        val uploadResources = config.resourceConfig.getConstructor().newInstance()
        val resourceConfig = (uploadResources as UploadResourceConfig).resources(config)

        httpServer = GrizzlyHttpServerFactory.createHttpServer(config.baseUri, resourceConfig)
        httpServer.start()
    }

    fun shutdown() {
        httpServer.shutdown().get(5, TimeUnit.SECONDS)
    }
}
