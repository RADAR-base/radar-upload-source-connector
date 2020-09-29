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
    mainClassName = "org.radarbase.upload.MainKt"
}

dependencies {
    api(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("org.radarbase:radar-jersey:${project.extra["radarJerseyVersion"]}")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${project.extra["jacksonVersion"]}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:${project.extra["jacksonVersion"]}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${project.extra["jacksonVersion"]}")

    implementation("org.slf4j:slf4j-api:${project.extra["slf4jVersion"]}")

    implementation("org.hibernate:hibernate-core:${project.extra["hibernateVersion"]}")
    implementation("org.hibernate:hibernate-c3p0:${project.extra["hibernateVersion"]}")
    implementation("org.liquibase:liquibase-core:${project.extra["liquibaseVersion"]}")

    implementation("com.squareup.okhttp3:okhttp:${project.extra["okhttpVersion"]}")

    runtimeOnly("com.h2database:h2:${project.extra["h2Version"]}")
    runtimeOnly("org.postgresql:postgresql:${project.extra["postgresqlVersion"]}")
    runtimeOnly("ch.qos.logback:logback-classic:${project.extra["logbackVersion"]}")

    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
    testImplementation("org.hamcrest:hamcrest-all:1.3")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.1.0")

    testImplementation("org.glassfish.jersey.test-framework.providers:jersey-test-framework-provider-grizzly2:${project.extra["jerseyVersion"]}")
}

// config JVM target to 1.8 for kotlin compilation tasks
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

allOpen {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.MappedSuperclass")
    annotation("javax.persistence.Embeddable")
}
