package org.jetbrains.spaceSlackSync.storage.postgres.impl

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.spaceSlackSync.storage.SlackTeamCached
import org.jetbrains.spaceSlackSync.storage.Storage
import org.jetbrains.spaceSlackSync.storage.postgres.DB
import org.jetbrains.spaceSlackSync.storage.postgres.StorageImpl

class SlackTeamCacheStorageImpl(db: Database) : StorageImpl(db), Storage.SlackTeamCache {
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
