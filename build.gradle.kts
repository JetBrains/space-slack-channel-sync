import com.github.gradle.node.npm.task.NpmTask
import java.util.*

val ktorVersion: String by project
val logbackVersion: String by project
val logbackJsonClassicVersion: String by project
val logbackJacksonVersion: String by project
val exposedVersion: String by project
val hikariVersion: String by project
val postgresqlDriverVersion: String by project
val awsSdkVersion: String by project
val spaceSdkVersion: String by project
val slackSdkVersion: String by project
val kotlinxSerializationVersion: String by project
val kotlinxHtmlJvmVersion: String by project
val kotlinxCoroutinesSlf4jVersion: String by project
val nimbusVersion: String by project

val spaceUsername: String? by extra
val spacePassword: String? by extra

plugins {
    application
    kotlin("jvm") version "1.7.0"
    kotlin("plugin.serialization") version "1.7.0"
    id("docker-compose")
    id("com.github.node-gradle.node") version "3.4.0"
}

node {
    version.set("16.15.1")
    download.set(true)
}

application {
    mainClass.set("io.ktor.server.jetty.EngineMain")
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://maven.pkg.jetbrains.space/public/p/space/maven")
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-locations-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-jetty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-apache-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-html-builder-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinxHtmlJvmVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:$kotlinxCoroutinesSlf4jVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("ch.qos.logback.contrib:logback-json-classic:$logbackJsonClassicVersion")
    implementation("ch.qos.logback.contrib:logback-jackson:$logbackJacksonVersion")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.postgresql:postgresql:$postgresqlDriverVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")

    implementation("org.jetbrains:space-sdk:${spaceSdkVersion}")

    implementation("com.nimbusds:nimbus-jose-jwt:$nimbusVersion")

    implementation("com.slack.api:slack-api-model:$slackSdkVersion")
    implementation("com.slack.api:slack-api-client:$slackSdkVersion")
    implementation("com.slack.api:slack-api-client-kotlin-extension:$slackSdkVersion")
    implementation("com.slack.api:slack-app-backend:$slackSdkVersion")
}

kotlin.sourceSets.all {
    languageSettings {
        optIn("kotlin.time.ExperimentalTime")
        optIn("io.ktor.server.locations.KtorExperimentalLocationsAPI")
        optIn("space.jetbrains.api.ExperimentalSpaceSdkApi")
    }
}

sourceSets {
    main {
        resources {
            srcDirs("client/build")
        }
    }
}

dockerCompose {
    projectName = "space-slack-sync"
    removeContainers = false
    removeVolumes = false
}

tasks.register("myNpmInstall", NpmTask::class) {
    npmCommand.set(listOf("install"))
    workingDir.set(File("./client"))
}

tasks.register("buildClient", NpmTask::class) {
    npmCommand.set(listOf("run", "build"))
    workingDir.set(File("./client"))

    dependsOn("myNpmInstall")
}

tasks {
    val run by getting(JavaExec::class) {
        systemProperties(readLocalProperties())
    }
    dockerCompose.isRequiredBy(run)

    val distZip by existing {
        dependsOn("buildClient")
    }
}

fun readLocalProperties(): Map<String, String> {
    val file = file(rootDir.absolutePath + "/local.properties")
    return if (file.exists()) {
        file.inputStream().use {
            val props = Properties().apply { load(it) }
            props.entries.associate { it.key.toString() to it.value.toString() }
        }
    } else {
        emptyMap()
    }
}

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}
