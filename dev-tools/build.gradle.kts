val ktorVersion: String by rootProject
val jacksonVersion: String by rootProject
val slackSdkVersion: String by rootProject
val spaceSdkVersion: String by rootProject

plugins {
    application
    kotlin("jvm") version "1.7.20"
}

application {
    mainClass.set("MainKt")
}

dependencies {
    implementation(project(":space-slack-sync"))
    implementation("io.ktor:ktor-client-apache-jvm:$ktorVersion")
    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.slack.api:slack-api-model:$slackSdkVersion")

    implementation("org.jetbrains:space-sdk:$spaceSdkVersion")
}
