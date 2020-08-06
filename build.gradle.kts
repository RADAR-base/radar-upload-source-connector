plugins {
    kotlin("jvm") version "1.3.72" apply false
}

subprojects {
    group = "org.radarbase"
    version = "0.5.2"

    project.extra.apply {
        set("kafkaVersion", "2.5.0")
        set("okhttpVersion", "4.8.0")
        set("jacksonVersion", "2.11.1")
        set("jacksonDataVersion", "2.11.1")
        set("openCsvVersion", "5.2")
        set("confluentVersion", "5.5.1")
        set("radarSchemaVersion", "0.5.11.1")
        set("slf4jVersion", "1.7.30")
        set("minioVersion", "7.1.0")

        set("radarJerseyVersion", "0.2.3")
        set("radarCommonsVersion", "0.13.0")
        set("logbackVersion", "1.2.3")
        set("grizzlyVersion", "2.4.4")
        set("jerseyVersion", "2.31")
        set("hibernateVersion", "5.4.19.Final")
        set("postgresqlVersion", "42.2.14")
        set("h2Version", "1.4.200")
        set("liquibaseVersion", "3.10.2")

        set("junitVersion", "5.6.2")
        set("mockitoKotlinVersion", "2.2.0")
    }
}

tasks.wrapper {
    gradleVersion = "6.5.1"
}
