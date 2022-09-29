import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    application
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.noarg")
    id("org.jetbrains.kotlin.plugin.jpa")
    id("org.jetbrains.kotlin.plugin.allopen")
}

application {
    mainClass.set("org.radarbase.upload.MainKt")
    applicationDefaultJvmArgs = listOf(
        "-Djava.security.egd=file:/dev/./urandom",
        "-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager",
    )
}

dependencies {
    api(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    val radarJerseyVersion: String by project
    implementation("org.radarbase:radar-jersey:$radarJerseyVersion") {
        exclude("io.swagger.core.v3", "swagger-jaxrs2")
    }
    implementation("org.radarbase:radar-jersey-hibernate:$radarJerseyVersion") {
        exclude("io.swagger.core.v3", "swagger-jaxrs2")
    }

    val jerseyVersion: String by project
    implementation("org.glassfish.jersey.containers:jersey-container-grizzly2-http:$jerseyVersion")

    val slf4jVersion: String by project
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    val log4j2Version: String by project
    runtimeOnly("org.apache.logging.log4j:log4j-core:$log4j2Version")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j2-impl:$log4j2Version")
    runtimeOnly("org.apache.logging.log4j:log4j-jul:$log4j2Version")

    val okhttpVersion: String by project
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")

    val hsqldbVersion: String by project
    runtimeOnly("org.hsqldb:hsqldb:${hsqldbVersion}")

    val junitVersion: String by project
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    val hamcrestVersion: String by project
    testImplementation("org.hamcrest:hamcrest:$hamcrestVersion")
    val mockitoKotlinVersion: String by project
    testImplementation("org.mockito.kotlin:mockito-kotlin:$mockitoKotlinVersion")

    testImplementation("org.glassfish.jersey.test-framework.providers:jersey-test-framework-provider-grizzly2:$jerseyVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

noArg {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}
