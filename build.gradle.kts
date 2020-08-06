plugins {
    kotlin("jvm") version "1.3.72" apply false
}

subprojects {
    group = "org.radarbase"
    version = "0.5.2-SNAPSHOT"
}

tasks.wrapper {
    gradleVersion = "6.5.1"
}
