import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.cli.common.toBooleanLenient
import java.time.Duration

plugins {
    java
    kotlin("jvm")
    id("com.avast.gradle.docker-compose") version "0.14.1"
}

sourceSets {
    create("integrationTest") {
        withConvention(KotlinSourceSet::class) {
            kotlin.srcDir("src/integrationTest/java")
            resources.srcDir("src/integrationTest/resources")
            compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
            runtimeClasspath += output + compileClasspath + sourceSets["test"].runtimeClasspath
        }
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testImplementation("com.fasterxml.jackson.core:jackson-databind:${project.extra["jacksonVersion"]}")
    testImplementation("com.squareup.okhttp3:okhttp:${project.extra["okhttpVersion"]}")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:${project.extra["jacksonDataVersion"]}")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${project.extra["jacksonDataVersion"]}")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:${project.extra["jacksonDataVersion"]}")

    testImplementation(project(":radar-upload-backend"))
    testImplementation(project(":kafka-connect-upload-source"))
    testImplementation("org.junit.jupiter:junit-jupiter:${project.extra["junitVersion"]}")
    testImplementation("org.hamcrest:hamcrest-all:1.3")
    testImplementation("org.apache.kafka:connect-api:${project.extra["kafkaVersion"]}")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:${project.extra["mockitoKotlinVersion"]}")
    testImplementation("org.mockito:mockito-core:${project.extra["mockitoCoreVersion"]}")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

task<Test>("integrationTest") {
    description = "Runs the integration tests"
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    mustRunAfter(tasks["test"])
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed", "standard_out", "standard_error")
        setExceptionFormat("full")
    }
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "11"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "11"
}

dockerCompose {
    useComposeFiles = listOf("docker-compose.yml")
    buildAdditionalArgs = emptyList<String>()
    val dockerComposeStopContainers: String? by project
    stopContainers = dockerComposeStopContainers?.toBooleanLenient() ?: true
    waitForTcpPortsTimeout = Duration.ofMinutes(3)
    environment["SERVICES_HOST"] = "localhost"
    isRequiredBy(tasks["integrationTest"])
}
