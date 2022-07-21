package org.jetbrains.spaceSlackSync

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import org.jetbrains.spaceSlackSync.platform.launch
import org.jetbrains.spaceSlackSync.routing.configureRouting
import org.jetbrains.spaceSlackSync.homepage.SlackTeamCache
import org.jetbrains.spaceSlackSync.storage.Storage
import org.jetbrains.spaceSlackSync.storage.postgres.initPostgres

@Suppress("unused")
fun Application.module() {
    db.apply {
    }

    launch {
        SlackTeamCache.clearCacheForAllTeams()
    }

    configureRouting()
}

val db: Storage by lazy {
    initPostgres()
        ?: error("Should specify connection config parameters for either DynamoDB or PostgreSQL")
}

val config: Config = ConfigFactory.load()

object SlackCredentials {
    val clientId: String = config.getString("slack.clientId").ifBlank { error("Slack client id should not be empty") }
    val clientSecret: String =
        config.getString("slack.clientSecret").ifBlank { error("Slack client secret should not be empty") }
    val signingSecret: String? = config.getString("slack.signingSecret").ifBlank { null }
    val appId: String = config.getString("slack.appId").ifBlank { error("Slack app id should not be empty") }
}
