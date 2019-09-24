import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    application
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.noarg") version "1.3.41"
    id("org.jetbrains.kotlin.plugin.jpa") version "1.3.41"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.3.41"
}

application {
    mainClassName = "org.radarbase.upload.MainKt"
}

project.extra.apply {
    set("okhttpVersion", "4.0.1")
    set("radarMpVersion", "0.5.7-SNAPSHOT")
    set("radarCommonsVersion", "0.12.2")
    set("radarSchemasVersion", "0.5.1")
    set("jacksonVersion", "2.9.9.1")
    set("jacksonDataVersion", "2.9.9")
    set("slf4jVersion", "1.7.27")
    set("logbackVersion", "1.2.3")
    set("grizzlyVersion", "2.4.4")
    set("jerseyVersion", "2.28")
    set("hibernateVersion", "5.4.4.Final")
}

repositories {
    jcenter()
    mavenLocal()
    maven(url = "https://dl.bintray.com/radar-cns/org.radarcns")
}

dependencies {
    compile(kotlin("stdlib-jdk8"))

    implementation("org.glassfish.grizzly:grizzly-http-server:${project.extra["grizzlyVersion"]}")

    implementation("org.glassfish.jersey.containers:jersey-container-grizzly2-http:${project.extra["jerseyVersion"]}")
    implementation("org.glassfish.jersey.containers:jersey-container-grizzly2-servlet:${project.extra["jerseyVersion"]}")
    implementation("org.glassfish.jersey.inject:jersey-hk2:${project.extra["jerseyVersion"]}")
    runtimeOnly("org.glassfish.jersey.media:jersey-media-json-jackson:${project.extra["jerseyVersion"]}")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${project.extra["jacksonDataVersion"]}")
    implementation("com.fasterxml.jackson.core:jackson-databind:${project.extra["jacksonVersion"]}")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${project.extra["jacksonDataVersion"]}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${project.extra["jacksonDataVersion"]}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:${project.extra["jacksonDataVersion"]}")

    implementation("org.radarcns:radar-auth:${project.extra["radarMpVersion"]}")
    implementation("org.radarbase:radar-auth-jersey:${project.extra["radarMpVersion"]}")

    implementation("org.slf4j:slf4j-api:${project.extra["slf4jVersion"]}")

    implementation("org.hibernate:hibernate-core:${project.extra["hibernateVersion"]}")
    implementation("org.hibernate:hibernate-c3p0:${project.extra["hibernateVersion"]}")
    implementation("org.liquibase:liquibase-core:3.5.3")

    implementation("com.squareup.okhttp3:okhttp:${project.extra["okhttpVersion"]}")

    runtimeOnly("com.h2database:h2:1.4.199")
    runtimeOnly("org.postgresql:postgresql:42.2.5")
    runtimeOnly("ch.qos.logback:logback-classic:${project.extra["logbackVersion"]}")


//    testImplementation("com.h2database:h2:1.4.199")
    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
    testImplementation("org.mockito:mockito-core:2.21.0")
    testImplementation("org.hamcrest:hamcrest-all:1.3")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.1.0")

}

// config JVM target to 1.8 for kotlin compilation tasks
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

allOpen {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.MappedSuperclass")
    annotation("javax.persistence.Embeddable")
}

tasks.register("downloadDependencies") {

    configurations["runtimeClasspath"].files
    configurations["compileClasspath"].files

    doLast {
        println("Downloaded all dependencies")
    }
}
