plugins {
    kotlin("jvm") version "1.3.41" apply false
}

subprojects {
    group = "org.radarbase"
    version = "0.1.0"
}

tasks.wrapper {
    gradleVersion = "5.5.1"
}
