plugins {
    kotlin("jvm") version "1.3.31" apply false
}

subprojects {
    group = "org.radarbase"
    version = "1.0.0-SNAPSHOT"
}

tasks.wrapper {
    gradleVersion = "5.4.1"
}
