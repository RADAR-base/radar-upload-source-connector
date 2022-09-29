import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm")
}

sourceSets {
    create("integrationTest") {
        withConvention(KotlinSourceSet::class) {
            kotlin.srcDir("src/integrationTest/java")
            resources.srcDir("src/integrationTest/resources")
            compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
            runtimeClasspath += output + compileClasspath + sourceSets["test"].runtimeClasspath
        }
    }
}

dependencies {
    val okhttpVersion: String by project
    api("com.squareup.okhttp3:okhttp:$okhttpVersion")
    val confluentVersion: String by project
    api("io.confluent:kafka-connect-avro-converter:$confluentVersion")
    val radarSchemaVersion: String by project
    api("org.radarbase:radar-schemas-commons:$radarSchemaVersion")

    val commonsCompressVersion: String by project
    implementation("org.apache.commons:commons-compress:$commonsCompressVersion")

    val jacksonVersion: String by project
    implementation(platform("com.fasterxml.jackson:jackson-bom:$jacksonVersion"))
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    val openCsvVersion: String by project
    implementation("com.opencsv:opencsv:$openCsvVersion")

    val jschVersion: String by project
    implementation("com.jcraft:jsch:$jschVersion")
    val minioVersion: String by project
    implementation("io.minio:minio:$minioVersion")

    // Included in connector runtime
    val kafkaVersion: String by project
    compileOnly("org.apache.kafka:connect-api:$kafkaVersion")
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    val junitVersion: String by project
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")

    testImplementation("io.confluent:kafka-connect-avro-converter:$confluentVersion")

    val hamcrestVersion: String by project
    testImplementation("org.hamcrest:hamcrest:$hamcrestVersion")
    testImplementation("org.apache.kafka:connect-api:$kafkaVersion")
    val mockitoKotlinVersion: String by project
    testImplementation("org.mockito.kotlin:mockito-kotlin:$mockitoKotlinVersion")
    val slf4jVersion: String by project
    testRuntimeOnly("org.slf4j:slf4j-simple:$slf4jVersion")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

// config JVM target to 1.8 for kotlin compilation tasks
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

task<Test>("integrationTest") {
    description = "Runs the integration tests"
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    mustRunAfter(tasks["test"])
}
