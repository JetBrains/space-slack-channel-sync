package org.jetbrains.spaceSlackSync.homepage

import org.jetbrains.spaceSlackSync.db
import org.jetbrains.spaceSlackSync.slack.SlackClient
import org.jetbrains.spaceSlackSync.slack.retrieveAllSlackChannels
import org.jetbrains.spaceSlackSync.storage.SlackChannelInfo
import org.jetbrains.spaceSlackSync.storage.SlackTeamCached

object SlackTeamCache {
    suspend fun getSlackTeam(slackClient: SlackClient, slackTeamId: String): SlackTeamCached {
        return db.slackTeamCache.getSlackTeamCache(slackTeamId)
            ?: calcAndStoreSlackTeamCached(slackClient, slackTeamId)
    }

    suspend fun clearCacheForAllTeams() {
        db.slackTeamCache.clearCacheForAllTeams()
    }

    suspend fun clearCacheForTeam(slackTeamId: String) {
        db.slackTeamCache.clearCacheForTeam(slackTeamId)
    }

    private suspend fun calcAndStoreSlackTeamCached(slackClient: SlackClient, slackTeamId: String): SlackTeamCached {
        val channels = retrieveAllSlackChannels(slackClient, slackTeamId)
            .map { SlackChannelInfo(it.id, it.name) }
        val slackTeamCached = SlackTeamCached(slackTeamId, channels)
        db.slackTeamCache.storeSlackTeamInCache(slackTeamCached)
        return slackTeamCached
    }
}
