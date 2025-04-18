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
}

dependencies {
    api(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("org.radarbase:radar-jersey:${Versions.radarJersey}") {
        exclude("io.swagger.core.v3", "swagger-jaxrs2")
    }
    implementation("org.radarbase:radar-jersey-hibernate:${Versions.radarJersey}") {
        exclude("io.swagger.core.v3", "swagger-jaxrs2")
    }

    implementation("org.glassfish.jersey.containers:jersey-container-grizzly2-http:${Versions.jersey}")

    implementation(enforcedPlatform("io.ktor:ktor-bom:${Versions.ktor}"))
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-cio")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-serialization-jackson")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${Versions.jackson}")

    runtimeOnly("org.hsqldb:hsqldb:${Versions.hsqldb}")

    testImplementation("com.squareup.okhttp3:okhttp:${Versions.okhttp}")
    testImplementation("org.radarbase:radar-auth:${Versions.managementPortal}")
    testImplementation("org.hamcrest:hamcrest:${Versions.hamcrest}")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${Versions.mockitoKotlin}")

    testImplementation("org.glassfish.jersey.test-framework.providers:jersey-test-framework-provider-grizzly2:${Versions.jersey}")
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
