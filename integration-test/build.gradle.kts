import org.jetbrains.kotlin.cli.common.toBooleanLenient
import java.time.Duration

plugins {
    id("com.avast.gradle.docker-compose") version Versions.dockerCompose
    id("org.radarbase.radar-kotlin")
}

sourceSets {
    create("integrationTest") {
        kotlin.srcDir("src/integrationTest/java")
        resources.setSrcDirs(listOf("src/integrationTest/resources"))
        compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
        runtimeClasspath += output + compileClasspath + sourceSets["test"].runtimeClasspath
    }
}

dependencies {
    testImplementation("io.confluent:kafka-connect-avro-converter:${Versions.confluent}")
    testImplementation(platform("com.fasterxml.jackson:jackson-bom:${Versions.jackson}"))
    testImplementation("com.squareup.okhttp3:okhttp:${Versions.okhttp}")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")

    testImplementation(project(":radar-upload-backend"))
    testImplementation(project(":kafka-connect-upload-source"))
    testImplementation("org.junit.jupiter:junit-jupiter:${Versions.junit}")
    testImplementation("org.hamcrest:hamcrest:${Versions.hamcrest}")
    testImplementation("org.apache.kafka:connect-api:${Versions.kafka}")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${Versions.mockitoKotlin}")
}

task<Test>("integrationTest") {
    description = "Runs the integration tests"
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    mustRunAfter(tasks["test"])
}

dockerCompose {
    useComposeFiles.set(listOf("docker-compose.yml"))
    buildAdditionalArgs.set(emptyList<String>())
    val dockerComposeStopContainers: String? by project
    stopContainers.set(dockerComposeStopContainers?.toBooleanLenient() ?: true)
    waitForTcpPortsTimeout.set(Duration.ofMinutes(3))
    environment.put("SERVICES_HOST", "localhost")
    isRequiredBy(tasks["integrationTest"])
}

radarKotlin {
    // TODO remove after using new release of radar-kotlin plugin
    javaVersion.set(Versions.java)
}
