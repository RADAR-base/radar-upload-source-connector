@Suppress("ConstPropertyName", "MemberVisibilityCanBePrivate")
object Versions {
    const val project = "0.6.4"

    const val java = 17
    const val kotlin = "2.3.20"
    const val wrapper = "8.13"
    const val dockerCompose = "0.17.5"

    const val radarCommons = "1.2.8"
    const val managementPortal = "3.0.1"
    const val confluent = "7.8.1"
    const val kafka = "$confluent-ce"

    // From image
    const val jackson = "2.21.7"
    const val ktor = "2.3.13"

    const val log4j2 = "2.25.5"
    const val sentryLog4j = "1.7.30"

    const val okhttp = "4.12.0"

    const val radarSchemas = "0.9.1"

    const val junit = "5.10.0"
    const val mockito = "5.3.1"

    const val openCsv = "5.11.2"
    const val minio = "8.5.10"
    // Forced transitive override for minio, see build.gradle.kts.
    const val bouncycastle = "1.85"
    // Maintained drop-in fork of the discontinued org.lz4:lz4-java.
    const val lz4 = "1.11.4"
    const val jsch = "0.1.55"
    const val radarJersey = "0.12.9"
    const val jersey = "3.1.3"
    const val hsqldb = "2.7.2"
    const val mockitoKotlin = "5.1.0"
    const val hamcrest = "2.2"
    const val commonsCompress = "1.26.0"
    const val xz = "1.9"
}
