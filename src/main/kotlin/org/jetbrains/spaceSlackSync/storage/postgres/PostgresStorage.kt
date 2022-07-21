package org.jetbrains.spaceSlackSync.storage.postgres

import io.ktor.http.*
import io.ktor.server.config.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.spaceSlackSync.config
import org.jetbrains.spaceSlackSync.decrypted
import org.jetbrains.spaceSlackSync.encrypted
import org.jetbrains.spaceSlackSync.storage.SlackTeam
import org.jetbrains.spaceSlackSync.storage.SlackTeamCached
import org.jetbrains.spaceSlackSync.storage.Storage
import org.jetbrains.spaceSlackSync.storage.SyncedChannel
import space.jetbrains.api.runtime.SpaceAppInstance
import java.time.LocalDateTime

class PostgresStorage(private val db: Database) : Storage {
    override val slackTeams = object : Storage.SlackTeams {
        override suspend fun getForSpaceOrg(spaceAppClientId: String): List<SlackTeam> {
            return tx {
                (DB.SlackTeams innerJoin DB.Slack2Space innerJoin DB.SpaceAppInstance)
                    .slice(DB.SlackTeams.columns)
                    .select { DB.SpaceAppInstance.clientId eq spaceAppClientId }
                    .map { it.toSlackTeam() }
            }
        }

        override suspend fun getById(teamId: String, spaceAppClientId: String?): SlackTeam? {
            return tx {
                if (spaceAppClientId != null) {
                    (DB.SlackTeams innerJoin DB.Slack2Space)
                        .slice(DB.SlackTeams.columns)
                        .select { (DB.SlackTeams.id eq teamId) and (DB.Slack2Space.spaceAppClientId eq spaceAppClientId) }
                        .firstOrNull()?.toSlackTeam()
                } else {
                    DB.SlackTeams.select { DB.SlackTeams.id eq teamId }.firstOrNull()?.toSlackTeam()
                }
            }
        }

        override suspend fun updateDomain(teamId: String, newDomain: String) {
            tx {
                DB.SlackTeams.update(
                    where = { DB.SlackTeams.id eq teamId },
                    body = { it[domain] = newDomain }
                )
            }
        }

        override suspend fun updateTokens(
            teamId: String,
            accessToken: ByteArray,
            refreshToken: ByteArray?,
            accessTokenExpiresAt: LocalDateTime
        ) {
            tx {
                DB.SlackTeams.update(
                    where = { DB.SlackTeams.id eq teamId },
                    body = {
                        it[DB.SlackTeams.accessToken] = ExposedBlob(accessToken)
                        it[DB.SlackTeams.accessTokenExpiresAt] = accessTokenExpiresAt
                        if (refreshToken != null) {
                            it[DB.SlackTeams.refreshToken] = ExposedBlob(refreshToken)
                        }
                    }
                )
            }
        }

        override suspend fun create(
            teamId: String,
            domain: String,
            spaceAppClientId: String,
            accessToken: ByteArray,
            refreshToken: ByteArray,
            accessTokenExpiresAt: LocalDateTime
        ) {
            tx {
                val teamExists = DB.SlackTeams.select { DB.SlackTeams.id eq teamId }.forUpdate().any()
                if (teamExists) {
                    DB.SlackTeams.update(
                        where = { DB.SlackTeams.id eq teamId },
                        body = {
                            it[DB.SlackTeams.accessToken] = ExposedBlob(accessToken)
                            it[DB.SlackTeams.accessTokenExpiresAt] = accessTokenExpiresAt
                            it[DB.SlackTeams.refreshToken] = ExposedBlob(refreshToken)
                        }
                    )
                } else {
                    DB.SlackTeams.insert {
                        it[id] = teamId
                        it[DB.SlackTeams.domain] = domain
                        it[created] = LocalDateTime.now()
                        it[DB.SlackTeams.accessToken] = ExposedBlob(accessToken)
                        it[DB.SlackTeams.accessTokenExpiresAt] = accessTokenExpiresAt
                        it[DB.SlackTeams.refreshToken] = ExposedBlob(refreshToken)
                    }
                }

                DB.Slack2Space.insertIgnore {
                    it[slackTeamId] = teamId
                    it[DB.Slack2Space.spaceAppClientId] = spaceAppClientId
                }
            }
        }

        override suspend fun disconnectFromSpaceOrg(teamId: String, spaceAppClientId: String) {
            tx {
                DB.Slack2Space.deleteWhere { (DB.Slack2Space.slackTeamId eq teamId) and (DB.Slack2Space.spaceAppClientId eq spaceAppClientId) }
            }
        }

        override suspend fun delete(teamId: String) {
            tx {
                DB.SlackTeams.deleteWhere { DB.SlackTeams.id eq teamId }
            }
        }


        private fun ResultRow.toSlackTeam() =
            SlackTeam(
                id = this[DB.SlackTeams.id].value,
                domain = this[DB.SlackTeams.domain],
                appAccessToken = this[DB.SlackTeams.accessToken].bytes,
                appRefreshToken = this[DB.SlackTeams.refreshToken].bytes,
                accessTokenExpiresAt = this[DB.SlackTeams.accessTokenExpiresAt]
            )
    }

    override val spaceAppInstances = object : Storage.SpaceAppInstances {
        override suspend fun save(spaceAppInstance: SpaceAppInstance) {
            tx {
                val domain = Url(spaceAppInstance.spaceServer.serverUrl).host
                DB.SpaceAppInstance.deleteWhere {
                    (DB.SpaceAppInstance.clientId eq spaceAppInstance.clientId) or (DB.SpaceAppInstance.domain eq domain)
                }
                DB.SpaceAppInstance.insert {
                    it[created] = LocalDateTime.now()
                    it[clientId] = spaceAppInstance.clientId
                    it[clientSecret] = ExposedBlob(spaceAppInstance.clientSecret.encrypted())
                    it[orgUrl] = spaceAppInstance.spaceServer.serverUrl
                    it[DB.SpaceAppInstance.domain] = domain
                }
            }
        }

        override suspend fun getById(clientId: String, slackTeamId: String?): SpaceAppInstance? {
            return tx {
                if (slackTeamId != null) {
                    (DB.SpaceAppInstance innerJoin DB.Slack2Space)
                        .slice(DB.SpaceAppInstance.columns)
                        .select { (DB.SpaceAppInstance.clientId eq clientId) and (DB.Slack2Space.slackTeamId eq slackTeamId) }
                        .firstOrNull()?.toSpaceOrg()
                } else {
                    DB.SpaceAppInstance
                        .select { DB.SpaceAppInstance.clientId eq clientId }
                        .firstOrNull()?.toSpaceOrg()
                }
            }
        }

        override suspend fun getByDomain(domain: String, slackTeamId: String): SpaceAppInstance? {
            return tx {
                (DB.SpaceAppInstance innerJoin DB.Slack2Space)
                    .slice(DB.SpaceAppInstance.columns)
                    .select { (DB.SpaceAppInstance.domain eq domain) and (DB.Slack2Space.slackTeamId eq slackTeamId) }
                    .map { it.toSpaceOrg() }
                    .firstOrNull()
            }
        }

        private fun ResultRow.toSpaceOrg() =
            SpaceAppInstance(
                clientId = this[DB.SpaceAppInstance.clientId],
                clientSecret = this[DB.SpaceAppInstance.clientSecret].bytes.decrypted(),
                spaceServerUrl = this[DB.SpaceAppInstance.orgUrl],
            )
    }

    override val syncedChannels = object : Storage.SyncedChannels {
        override suspend fun getByAppClientId(spaceAppClientId: String): List<SyncedChannel> {
            return tx {
                DB.SyncedChannels.select { DB.SyncedChannels.spaceAppClientId eq spaceAppClientId }
                    .map { toSyncedChannel(it) }
            }
        }

        override suspend fun getByIds(
            spaceAppClientId: String,
            spaceChannelId: String,
            slackTeamId: String,
            slackChannelId: String
        ): SyncedChannel? {
            return tx {
                DB.SyncedChannels.select {
                    (DB.SyncedChannels.slackTeamId eq slackTeamId) and (DB.SyncedChannels.spaceAppClientId eq spaceAppClientId) and (DB.SyncedChannels.slackChannelId eq slackChannelId) and (DB.SyncedChannels.spaceChannelId eq spaceChannelId)
                }
                    .map { toSyncedChannel(it) }
                    .firstOrNull()
            }
        }

        override suspend fun getBySlackTeamId(slackTeamId: String): List<SyncedChannel> {
            return tx {
                DB.SyncedChannels.select {
                    DB.SyncedChannels.slackTeamId eq slackTeamId
                }
                    .map { toSyncedChannel(it) }
            }
        }

        override suspend fun getBySlackChannelId(slackTeamId: String, slackChannelId: String): SyncedChannel? {
            return tx {
                DB.SyncedChannels.select {
                    (DB.SyncedChannels.slackTeamId eq slackTeamId) and (DB.SyncedChannels.slackChannelId eq slackChannelId)
                }
                    .map { toSyncedChannel(it) }
                    .firstOrNull()
            }
        }

        override suspend fun addIfAbsent(
            spaceAppClientId: String,
            spaceChannelId: String,
            slackTeamId: String,
            slackChannelId: String
        ) {
            // TODO: do it in a single transaction, set isolation level = repeated read in Postgres
            if (getBySlackChannelId(slackTeamId, slackChannelId) != null) {
                return
            }

            tx {
                DB.SyncedChannels.insertIgnore {
                    it[DB.SyncedChannels.slackTeamId] = slackTeamId
                    it[DB.SyncedChannels.slackChannelId] = slackChannelId
                    it[DB.SyncedChannels.spaceAppClientId] = spaceAppClientId
                    it[DB.SyncedChannels.spaceChannelId] = spaceChannelId
                }
            }
        }

        override suspend fun remove(
            spaceAppClientId: String,
            spaceChannelId: String,
            slackTeamId: String,
            slackChannelId: String
        ) {
            tx {
                DB.SyncedChannels.deleteWhere {
                    (DB.SyncedChannels.slackTeamId eq slackTeamId) and (DB.SyncedChannels.spaceAppClientId eq spaceAppClientId) and (DB.SyncedChannels.slackChannelId eq slackChannelId) and (DB.SyncedChannels.spaceChannelId eq spaceChannelId)
                }
            }
        }

        private fun toSyncedChannel(it: ResultRow) = SyncedChannel(
            slackTeamId = it[DB.SyncedChannels.slackTeamId],
            slackChannelId = it[DB.SyncedChannels.slackChannelId],
            spaceAppClientId = it[DB.SyncedChannels.spaceAppClientId],
            spaceChannelId = it[DB.SyncedChannels.spaceChannelId],
        )
    }

    override val messages = object : Storage.Messages {
        override suspend fun getSlackMsgBySpaceMsg(spaceMessageId: String): String? {
            return tx {
                DB.SpaceToSlackMessage
                    .slice(DB.SpaceToSlackMessage.slackMessageId)
                    .select {
                        DB.SpaceToSlackMessage.spaceMessageId eq spaceMessageId
                    }
                    .map { it[DB.SpaceToSlackMessage.slackMessageId] }
                    .firstOrNull()
            }
        }

        override suspend fun getSpaceMsgBySlackMsg(slackMessageId: String): String? {
            return tx {
                DB.SpaceToSlackMessage
                    .slice(DB.SpaceToSlackMessage.spaceMessageId)
                    .select {
                        DB.SpaceToSlackMessage.slackMessageId eq slackMessageId
                    }
                    .map { it[DB.SpaceToSlackMessage.spaceMessageId] }
                    .firstOrNull()
            }
        }

        override suspend fun setSlackMsgBySpaceMsg(
            slackTeamId: String,
            slackMessageId: String,
            spaceMessageId: String
        ) {
            tx {
                DB.SpaceToSlackMessage.insertIgnore {
                    it[DB.SpaceToSlackMessage.slackTeamId] = slackTeamId
                    it[DB.SpaceToSlackMessage.slackMessageId] = slackMessageId
                    it[DB.SpaceToSlackMessage.spaceMessageId] = spaceMessageId
                }
            }
        }
    }

    override val slackTeamCache = object : Storage.SlackTeamCache {
        override suspend fun getSlackTeamCache(slackTeamId: String): SlackTeamCached? {
            return tx {
                val slackTeamSerialized = DB.SlackTeamCache
                    .slice(DB.SlackTeamCache.teamCache)
                    .select {
                        DB.SlackTeamCache.slackTeamId eq slackTeamId
                    }
                    .map { it[DB.SlackTeamCache.teamCache] }
                    .firstOrNull()
                    ?: return@tx null

                Json.decodeFromString(slackTeamSerialized)
            }
        }

        override suspend fun storeSlackTeamInCache(slackTeamCached: SlackTeamCached) {
            val teamSerialized = Json.encodeToString(slackTeamCached)
            tx {
                DB.SlackTeamCache.replace {
                    it[slackTeamId] = slackTeamCached.id
                    it[teamCache] = teamSerialized
                }
            }
        }

        override suspend fun clearCacheForAllTeams() {
            tx {
                DB.SlackTeamCache.deleteAll()
            }
        }

        override suspend fun clearCacheForTeam(slackTeamId: String) {
            tx {
                DB.SlackTeamCache.deleteWhere {
                    DB.SlackTeamCache.slackTeamId eq slackTeamId
                }
            }
        }
    }

    private fun <T> tx(statement: Transaction.() -> T): T =
        transaction(db, statement)
}

fun initPostgres(): PostgresStorage? {
    val postgresUrl = config.tryGetString("storage.postgres.url")?.let { Url(it) } ?: return null

    val connection = Database.connect(
        url = URLBuilder(postgresUrl).apply {
            protocol = URLProtocol("jdbc:postgresql", 5432)
            port = postgresUrl.port
            user = null
            password = null
        }.buildString(),
        driver = "org.postgresql.Driver",
        user = postgresUrl.user!!,
        password = postgresUrl.password!!
    )

    transaction(connection) {
        SchemaUtils.createMissingTablesAndColumns(*allTables.toTypedArray())
    }

    return PostgresStorage(connection)
}
