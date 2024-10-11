plugins {
    java
    kotlin("jvm")
}

sourceSets {
    create("integrationTest") {
        kotlin.srcDir("src/integrationTest/java")
        resources.srcDir("src/integrationTest/resources")
        compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
        runtimeClasspath += output + compileClasspath + sourceSets["test"].runtimeClasspath
    }
}

dependencies {
    api("com.squareup.okhttp3:okhttp:${Versions.okhttp}")
    api("io.confluent:kafka-connect-avro-converter:${Versions.confluent}")
    api("org.radarbase:radar-schemas-commons:${Versions.radarSchemas}")

    implementation("org.apache.commons:commons-compress:${Versions.commonsCompress}")
    implementation("org.tukaani:xz:${Versions.xz}")

    implementation(platform("com.fasterxml.jackson:jackson-bom:${Versions.jackson}"))
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("com.opencsv:opencsv:${Versions.openCsv}")

    implementation("com.jcraft:jsch:${Versions.jsch}")
    implementation("io.minio:minio:${Versions.minio}")

    // Included in connector runtime
    compileOnly("org.apache.kafka:connect-api:${Versions.kafka}")
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    testImplementation("io.confluent:kafka-connect-avro-converter:${Versions.confluent}")

    testImplementation("org.hamcrest:hamcrest:${Versions.hamcrest}")
    testImplementation("org.apache.kafka:connect-api:${Versions.kafka}")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${Versions.mockitoKotlin}")
}

task<Test>("integrationTest") {
    description = "Runs the integration tests"
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    mustRunAfter(tasks["test"])
}
