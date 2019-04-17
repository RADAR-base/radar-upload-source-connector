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
