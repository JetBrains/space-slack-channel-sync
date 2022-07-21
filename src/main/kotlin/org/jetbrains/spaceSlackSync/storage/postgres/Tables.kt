package org.jetbrains.spaceSlackSync.storage.postgres

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime

val allTables = listOf(
    DB.SpaceAppInstance,
    DB.SlackTeams,
    DB.Slack2Space,
    DB.SyncedChannels,
    DB.SpaceToSlackMessage,
    DB.SlackTeamCache,
)

object DB {
    object SpaceAppInstance : Table("SpaceAppInstance") {
        val clientId = varchar("clientId", 36)
        val domain = varchar("domain", 256)
        val clientSecret = blob("clientSecret")
        val orgUrl = varchar("orgUrl", 256)
        val created = datetime("created")

        override val primaryKey = PrimaryKey(clientId)
    }

    object SlackTeams : IdTable<String>("SlackTeams") {
        override val id = varchar("id", 100).entityId()
        override val primaryKey = PrimaryKey(id)

        val domain = varchar("domain", 256).uniqueIndex()
        val accessToken = blob("accessToken")
        val refreshToken = blob("refreshToken")
        val accessTokenExpiresAt = datetime("accessTokenExpiresAt")
        val created = datetime("created")
    }

    object Slack2Space : Table("Slack2Space") {
        val spaceAppClientId =
            varchar("spaceAppClientId", 36).references(SpaceAppInstance.clientId, onDelete = ReferenceOption.CASCADE)
        val slackTeamId = varchar("slackTeamId", 100).references(SlackTeams.id, onDelete = ReferenceOption.CASCADE)

        override val primaryKey = PrimaryKey(spaceAppClientId, slackTeamId)

        init {
            uniqueIndex(slackTeamId, spaceAppClientId)
        }
    }

    object SyncedChannels : Table("SyncedChannels") {
        val slackTeamId = varchar("slackTeamId", 100).references(SlackTeams.id, onDelete = ReferenceOption.CASCADE)
        val slackChannelId = varchar("slackChannelId", 256)
        val spaceAppClientId =
            varchar("spaceAppClientId", 36).references(SpaceAppInstance.clientId, onDelete = ReferenceOption.CASCADE)
        val spaceChannelId = varchar("spaceChannelId", 256)

        init {
            uniqueIndex(slackTeamId, slackChannelId)
        }
    }

    object SpaceToSlackMessage : Table("SpaceToSlackMessage") {
        val slackTeamId = varchar("slackTeamId", 100).references(SlackTeams.id, onDelete = ReferenceOption.CASCADE)
        val spaceMessageId = varchar("spaceMessageId", 100)
        val slackMessageId = varchar("slackMessageId", 100)

        init {
            uniqueIndex(spaceMessageId, slackMessageId)
            index(isUnique = false, slackMessageId, spaceMessageId)
        }
    }

    object SlackTeamCache : Table("SlackTeamCache") {
        val slackTeamId = varchar("slackTeamId", 100).references(SlackTeams.id, onDelete = ReferenceOption.CASCADE)
        val teamCache = text("teamCache")

        override val primaryKey = PrimaryKey(slackTeamId)
        init {
            uniqueIndex(slackTeamId)
        }
    }
}
