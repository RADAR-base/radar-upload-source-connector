import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") apply false
    id("com.github.ben-manes.versions") version "0.42.0"
}

allprojects {
    group = "org.radarbase"
    version = "0.5.12"
}

subprojects {
    repositories {
        mavenCentral()
        maven(url = "https://packages.confluent.io/maven/")
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
            configurations.find { it.name =="compileClasspath" }?.files
            configurations.find { it.name =="runtimeClasspath" }?.files
        }

        tasks.register<Copy>("copyDependencies") {
            try {
                from(
                    configurations.named("runtimeClasspath").map { it.files }
                )
            } catch (ex: UnknownDomainObjectException) {
                // do nothing
            }
            into("$buildDir/third-party/")
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
    gradleVersion = "7.5.1"
}
