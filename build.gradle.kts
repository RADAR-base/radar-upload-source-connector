plugins {
    kotlin("jvm") version "1.3.41" apply false
}

subprojects {
    group = "org.radarbase"
    version = "0.2.0"
}

tasks.wrapper {
    gradleVersion = "5.6.2"
}
