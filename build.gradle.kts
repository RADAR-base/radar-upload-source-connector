plugins {
    id("org.radarbase.radar-root-project") version Versions.radarCommons
    id("org.radarbase.radar-dependency-management") version Versions.radarCommons
    id("org.radarbase.radar-kotlin") version Versions.radarCommons apply false
}

allprojects {
    group = "org.radarbase"
    version = "0.6.2"

    configurations.configureEach {
        /* The entries in the block below are added here to force the version of
         * transitive dependencies and mitigate reported vulnerabilities
         */
        resolutionStrategy {
            force(
                "org.apache.commons:commons-lang3:3.18.0"
            )
        }
    }
}

radarRootProject {
    projectVersion.set(Versions.project)
    gradleVersion.set(Versions.wrapper)
}
