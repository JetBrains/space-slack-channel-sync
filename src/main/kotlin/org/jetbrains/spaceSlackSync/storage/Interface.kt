package org.jetbrains.spaceSlackSync.storage

import kotlinx.serialization.Serializable
import space.jetbrains.api.runtime.SpaceAppInstance
import java.time.LocalDateTime

interface Storage {
    val slackTeams: SlackTeams

    val spaceAppInstances: SpaceAppInstances

    val syncedChannels: SyncedChannels

    val messages: Messages

    val slackTeamCache: SlackTeamCache

    interface SlackTeams {
        suspend fun getForSpaceOrg(spaceAppClientId: String): List<SlackTeam>
        suspend fun getById(teamId: String, spaceAppClientId: String? = null): SlackTeam?
        suspend fun createOrUpdate(
            teamId: String,
            domain: String,
            spaceAppClientId: String,
            accessToken: ByteArray,
            refreshToken: ByteArray,
            accessTokenExpiresAt: LocalDateTime
        )

        suspend fun updateDomain(teamId: String, newDomain: String)
        suspend fun updateTokens(teamId: String, accessToken: ByteArray, refreshToken: ByteArray?, accessTokenExpiresAt: LocalDateTime)
        suspend fun disconnectFromSpaceOrg(teamId: String, spaceAppClientId: String)
        suspend fun markTokenAsInvalid(teamId: String)
    }

    interface SpaceAppInstances {
        suspend fun save(spaceAppInstance: SpaceAppInstance)
        suspend fun getById(clientId: String, slackTeamId: String? = null): SpaceAppInstance?
        suspend fun getByDomain(domain: String, slackTeamId: String): SpaceAppInstance?
    }

    interface SyncedChannels {
        suspend fun getByAppClientId(spaceAppClientId: String): List<SyncedChannel>

        suspend fun getByIds(spaceAppClientId: String, spaceChannelId: String, slackTeamId: String, slackChannelId: String): SyncedChannel?

        suspend fun getBySlackTeamId(slackTeamId: String): List<SyncedChannel>

        suspend fun getBySlackChannelId(slackTeamId: String, slackChannelId: String): SyncedChannel?

        suspend fun addIfAbsent(spaceAppClientId: String, spaceChannelId: String, slackTeamId: String, slackChannelId: String)

        suspend fun remove(spaceAppClientId: String, spaceChannelId: String, slackTeamId: String, slackChannelId: String)
    }

    interface Messages {
        suspend fun getSlackMsgBySpaceMsg(spaceMessageId: String): String?
        suspend fun getSpaceMsgBySlackMsg(slackMessageId: String): String?
        suspend fun setSlackMsgBySpaceMsg(slackTeamId: String, slackMessageId: String, spaceMessageId: String)
    }

    interface SlackTeamCache {
        suspend fun getSlackTeamCache(slackTeamId: String): SlackTeamCached?

        suspend fun storeSlackTeamInCache(slackTeamCached: SlackTeamCached)

        suspend fun clearCacheForAllTeams()

        suspend fun clearCacheForTeam(slackTeamId: String)
    }
}

class SlackTeam(val id: String, val domain: String, val appAccessToken: ByteArray, val appRefreshToken: ByteArray, val accessTokenExpiresAt: LocalDateTime)

@Serializable
class SyncedChannel(
    val slackTeamId: String,
    val slackChannelId: String,
    val spaceAppClientId: String,
    val spaceChannelId: String,
)

@Serializable
class SlackChannelInfo(
    val id: String,
    val name: String,
)

@Serializable
class SlackTeamCached(
    val id: String,
    val channels: List<SlackChannelInfo>
)
