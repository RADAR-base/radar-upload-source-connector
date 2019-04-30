import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.3.30"
}

project.extra.apply {
    set("okhttpVersion", "3.14.1")
    set("kafkaVersion", "2.2.0-cp2")
    set("jacksonVersion", "2.9.8")
}

repositories {
    jcenter()
    maven(url = "http://packages.confluent.io/maven/")
    maven(url = "https://dl.bintray.com/radar-cns/org.radarcns")
}

dependencies {

    implementation(kotlin("stdlib-jdk8"))
    testImplementation("com.fasterxml.jackson.core:jackson-databind:${project.extra["jacksonVersion"]}")
    testImplementation("com.squareup.okhttp3:okhttp:${project.extra["okhttpVersion"]}")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:${project.extra["jacksonVersion"]}")


    testImplementation(project(":radar-upload-backend"))
    testImplementation(project(":kafka-connect-upload-source"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
    testImplementation("org.hamcrest:hamcrest-all:1.3")
    testImplementation("org.apache.kafka:connect-api:${project.extra["kafkaVersion"]}")
}

// config JVM target to 1.8 for kotlin compilation tasks
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
