package org.jetbrains.spaceSlackSync.storage.postgres.impl

import org.jetbrains.exposed.sql.*
import org.jetbrains.spaceSlackSync.storage.Storage
import org.jetbrains.spaceSlackSync.storage.SyncedChannel
import org.jetbrains.spaceSlackSync.storage.postgres.DB
import org.jetbrains.spaceSlackSync.storage.postgres.StorageImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SyncedChannelsStorageImpl(db: Database) : StorageImpl(db), Storage.SyncedChannels {
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

    override suspend fun getBySlackChannel(slackTeamId: String, slackChannelId: String): SyncedChannel? {
        return tx {
            DB.SyncedChannels.select {
                (DB.SyncedChannels.slackTeamId eq slackTeamId) and (DB.SyncedChannels.slackChannelId eq slackChannelId)
            }
                .map { toSyncedChannel(it) }
                .firstOrNull()
        }
    }

    override suspend fun getBySpaceChannel(spaceAppClientId: String, spaceChannelId: String): SyncedChannel? {
        return tx {
            DB.SyncedChannels.select {
                (DB.SyncedChannels.spaceAppClientId eq spaceAppClientId) and (DB.SyncedChannels.spaceChannelId eq spaceChannelId)
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
        if (getBySlackChannel(slackTeamId, slackChannelId) != null) {
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

private val log: Logger = LoggerFactory.getLogger(SyncedChannelsStorageImpl::class.java)
