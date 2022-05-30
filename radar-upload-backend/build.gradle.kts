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

    val slf4jVersion: String by project
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    val log4j2Version: String by project
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version")
    runtimeOnly("org.apache.logging.log4j:log4j-api:$log4j2Version")
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

    val jerseyVersion: String by project
    testImplementation("org.glassfish.jersey.test-framework.providers:jersey-test-framework-provider-grizzly2:$jerseyVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

allOpen {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.MappedSuperclass")
    annotation("javax.persistence.Embeddable")
}
