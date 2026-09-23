plugins {
    id("org.radarbase.radar-root-project") version Versions.radarCommons
    id("org.radarbase.radar-dependency-management") version Versions.radarCommons
    id("org.radarbase.radar-kotlin") version Versions.radarCommons apply false
}

allprojects {
    group = "org.radarbase"
    version = "0.6.4"

    configurations.configureEach {
        /* The entries in the block below are added here to force the version of
         * transitive dependencies and mitigate reported vulnerabilities
         */
        resolutionStrategy {
            force(
                "org.apache.commons:commons-lang3:3.18.0",
                // minio pulls in a vulnerable bcprov (CVE-2025-14813, CVE-2026-13506, CVE-2026-8763).
                "org.bouncycastle:bcprov-jdk18on:${Versions.bouncycastle}",
            )
            dependencySubstitution {
                // CVE-2025-12183, CVE-2025-66566: org.lz4 is discontinued, use the maintained fork.
                substitute(module("org.lz4:lz4-java"))
                    .using(module("at.yawk.lz4:lz4-java:${Versions.lz4}"))
                    .because("CVE-2025-12183, CVE-2025-66566")
            }
        }
    }
}

radarRootProject {
    projectVersion.set(Versions.project)
    gradleVersion.set(Versions.wrapper)
}
