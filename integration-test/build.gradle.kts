import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm")
}

project.extra.apply {
    set("okhttpVersion", "4.0.1")
    set("kafkaVersion", "2.3.0")
    set("jacksonVersion", "2.9.9.1")
    set("jacksonDataVersion", "2.9.9")
}

repositories {
    jcenter()
    mavenLocal()
    maven(url = "http://packages.confluent.io/maven/")
    maven(url = "https://dl.bintray.com/radar-cns/org.radarcns")
    maven(url = "https://dl.bintray.com/radar-base/org.radarbase")
    maven(url = "https://repo.thehyve.nl/content/repositories/snapshots")
    maven(url = "http://oss.jfrog.org/artifactory/oss-snapshot-local/")
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
    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
    testImplementation("org.hamcrest:hamcrest-all:1.3")
    testImplementation("org.apache.kafka:connect-api:${project.extra["kafkaVersion"]}")
}

// config JVM target to 1.8 for kotlin compilation tasks
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
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
