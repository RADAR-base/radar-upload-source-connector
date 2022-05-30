import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.cli.common.toBooleanLenient
import java.time.Duration

plugins {
    java
    kotlin("jvm")
    id("com.avast.gradle.docker-compose") version "0.16.4"
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
    val jacksonVersion: String by project
    testImplementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    val okhttpVersion: String by project
    testImplementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")

    testImplementation(project(":radar-upload-backend"))
    testImplementation(project(":kafka-connect-upload-source"))
    val junitVersion: String by project
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    val hamcrestVersion: String by project
    testImplementation("org.hamcrest:hamcrest:$hamcrestVersion")
    val kafkaVersion: String by project
    testImplementation("org.apache.kafka:connect-api:$kafkaVersion")
    val mockitoKotlinVersion: String by project
    testImplementation("org.mockito.kotlin:mockito-kotlin:$mockitoKotlinVersion")
}

task<Test>("integrationTest") {
    description = "Runs the integration tests"
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    mustRunAfter(tasks["test"])
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
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
