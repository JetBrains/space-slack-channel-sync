import com.github.gradle.node.npm.task.NpmTask
import java.util.*

val ktorVersion: String by rootProject
val logbackVersion: String by rootProject
val logbackEncoderVersion: String by rootProject
val exposedVersion: String by rootProject
val hikariVersion: String by rootProject
val postgresqlDriverVersion: String by rootProject
val spaceSdkVersion: String by rootProject
val slackSdkVersion: String by rootProject
val kotlinxSerializationVersion: String by rootProject
val kotlinxHtmlJvmVersion: String by rootProject
val kotlinxCoroutinesSlf4jVersion: String by rootProject
val nimbusVersion: String by rootProject

plugins {
    application
    kotlin("jvm") version "1.7.21"
    kotlin("plugin.serialization") version "1.7.21"
    id("docker-compose")
    id("com.github.node-gradle.node") version "3.5.1"
}

node {
    version.set("19.5.0")
    download.set(true)

    /**
     * By default `node` plugin declares a repository to download Node.js from. This prevents that.
     * The ivy repository is instead declared in `settings.gradle`.
     * See also: https://github.com/node-gradle/gradle-node-plugin/blob/master/docs/faq.md#is-this-plugin-compatible-with-centralized-repositories-declaration
     */
    distBaseUrl.set(null as String?)
}

application {
    mainClass.set("io.ktor.server.jetty.EngineMain")
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
    implementation("net.logstash.logback:logstash-logback-encoder:$logbackEncoderVersion")

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

dockerCompose {
    projectName = "space-slack-sync"
    removeContainers = false
    removeVolumes = false
}

tasks.register("clientNpmInstall", NpmTask::class) {
    npmCommand.set(listOf("install"))
    workingDir.set(File("./client"))
}

val buildClientTask = tasks.register("buildClient", NpmTask::class) {
    npmCommand.set(listOf("run", "build"))
    workingDir.set(File("./client"))

    dependsOn("clientNpmInstall")
    inputs.dir("client")

    /**
     * Declare output directory of this task (see usages of the `buildClientTask`).
     * The actual instruction to use the `build/client` directory as an output is
     * written in `client/.env` file. It would be nice to not have to write this two times.
     */
    outputs.dir("build/client")
}

sourceSets {
    main {
        resources {
            srcDir(buildClientTask)
        }
    }
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
