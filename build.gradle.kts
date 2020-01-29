plugins {
    kotlin("jvm") version "1.3.61" apply false
}

subprojects {
    group = "org.radarbase"
    version = "0.4.1-SNAPSHOT"
}

tasks.wrapper {
    gradleVersion = "6.0.1"
}
