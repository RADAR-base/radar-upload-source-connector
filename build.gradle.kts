import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") apply false
    id("com.github.ben-manes.versions") version "0.39.0"
}

allprojects {
    group = "org.radarbase"
    version = "0.5.10"
}

subprojects {
    repositories {
        mavenCentral()
        maven(url = "https://packages.confluent.io/maven/")
        maven(url = "https://repo.thehyve.nl/content/repositories/snapshots")
    }

    val kotlinApiVersion: String by project

    afterEvaluate {
        tasks.withType<Test> {
            useJUnitPlatform()
            testLogging {
                events("passed", "skipped", "failed")
                setExceptionFormat("full")
                showStandardStreams = true
                exceptionFormat = FULL
            }
        }

        tasks.withType<KotlinCompile> {
            kotlinOptions {
                apiVersion = kotlinApiVersion
                languageVersion = kotlinApiVersion
            }
        }


        tasks.register("downloadDependencies") {
            configurations["runtimeClasspath"].files
            configurations["compileClasspath"].files

            doLast {
                println("Downloaded all dependencies")
            }
        }
    }
}

val stableVersionRegex = "[0-9,.v-]+(-r)?".toRegex()

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA")
        .any { version.toUpperCase().contains(it) }
    return !stableKeyword && !stableVersionRegex.matches(version)
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}

tasks.wrapper {
    gradleVersion = "7.3.1"
}
