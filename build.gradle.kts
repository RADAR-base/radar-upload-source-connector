import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") apply false
}


subprojects {
    group = "org.radarbase"
    version = "0.5.8"

    project.extra.apply {
        set("kafkaVersion", "2.5.1")
        set("okhttpVersion", "4.9.0")
        set("jacksonVersion", "2.11.4")
        set("jacksonDataVersion", "2.11.4")
        set("openCsvVersion", "5.4")
        set("confluentVersion", "5.5.3")
        set("radarSchemaVersion", "0.5.15")
        set("slf4jVersion", "1.7.30")
        set("minioVersion", "7.1.4")
        set("radarJerseyVersion", "0.4.3")
        set("radarCommonsVersion", "0.13.0")
        set("logbackVersion", "1.2.3")
        set("jerseyVersion", "2.33")
        set("h2Version", "1.4.200")

        set("junitVersion", "5.6.2")
        set("mockitoKotlinVersion", "2.2.0")
        set("mockitoCoreVersion", "3.8.0")
    }

    repositories {
        jcenter()
        maven(url = "https://packages.confluent.io/maven/")
        maven(url = "https://dl.bintray.com/radar-cns/org.radarcns")
        maven(url = "https://dl.bintray.com/radar-base/org.radarbase")
        maven(url = "https://repo.thehyve.nl/content/repositories/snapshots")
        maven(url = "https://oss.jfrog.org/artifactory/oss-snapshot-local/")
    }

    val kotlinApiVersion: String by project

    afterEvaluate {
        tasks.withType<Test> {
            useJUnitPlatform()
            testLogging {
                events("passed", "skipped", "failed")
                setExceptionFormat("full")
                showStandardStreams = true
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

tasks.wrapper {
    gradleVersion = "6.8.3"
}
