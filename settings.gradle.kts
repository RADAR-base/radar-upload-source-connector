rootProject.name = "radar-upload-source-connector"
include(":radar-upload-backend")
include(":kafka-connect-upload-source")
include(":integration-test")

pluginManagement {
    val kotlinVersion: String by settings
    val dockerComposeVersion: String by settings
    plugins {
        kotlin("jvm") version kotlinVersion
        id("org.jetbrains.kotlin.plugin.noarg") version kotlinVersion
        id("org.jetbrains.kotlin.plugin.jpa") version kotlinVersion
        id("org.jetbrains.kotlin.plugin.allopen") version kotlinVersion
        id("com.avast.gradle.docker-compose") version dockerComposeVersion
    }
}
