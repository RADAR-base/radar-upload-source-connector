package org.radarbase.upload

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess


val logger: Logger = LoggerFactory.getLogger("org.radarbase.upload.Main")

fun loadConfig(args: Array<String>): Config {
    val configFile = when {
        args.size == 1 -> args[0]
        Files.exists(Paths.get("upload.yml")) -> "upload.yml"
        else -> null
    }?.let { File(it) }

    return configFile?.let {
        logger.info("Reading configuration from ${it.absolutePath}")
        try {
            val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())
            mapper.readValue<Config>(it)
        } catch (ex: IOException) {
            logger.error("Usage: radar-upload [upload.yml]")
            logger.error("Failed to read config file $configFile: ${ex.message}")
            exitProcess(1)
        }
    } ?: Config()
}

fun main(args: Array<String>) {
    val config = loadConfig(args)
    val server = GrizzlyServer(config)

    // register shutdown hook
    Runtime.getRuntime().addShutdownHook(Thread(Runnable {
        logger.info("Stopping server..")
        server.shutdown()
    }, "shutdownHook"))

    try {
        server.start()

        logger.info(String.format("Jersey app started on %s.\nPress Ctrl+C to exit...",
                config.baseUri))
        Thread.currentThread().join()
    } catch (e: Exception) {
        logger.error("Error starting server: $e")
    }
}
