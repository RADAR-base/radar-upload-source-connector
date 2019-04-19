import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.3.30"
}

project.extra.apply {
    set("kafkaVersion", "2.2.0-cp2")
    set("okhttpVersion", "3.14.1")
    set("jacksonVersion", "2.8.9")
}

repositories {
    jcenter()
    maven(url = "http://packages.confluent.io/maven/")
    maven(url = "https://dl.bintray.com/radar-cns/org.radarcns")
}

dependencies {
    api("com.squareup.okhttp3:okhttp:${project.extra["okhttpVersion"]}")
    implementation("com.fasterxml.jackson.dataformat:jackson-datatype-jsr310:${project.extra["jacksonVersion"]}")
    implementation("com.fasterxml.jackson.core:jackson-databind:${project.extra["jacksonVersion"]}")


    // Included in connector runtime
    compileOnly("org.apache.kafka:connect-api:${project.extra["kafkaVersion"]}")
    implementation(kotlin("stdlib-jdk8"))
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
