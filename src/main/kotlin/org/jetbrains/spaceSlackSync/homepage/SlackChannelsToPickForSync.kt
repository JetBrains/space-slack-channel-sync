package org.jetbrains.spaceSlackSync.homepage

import kotlinx.serialization.Serializable
import org.jetbrains.spaceSlackSync.db
import org.jetbrains.spaceSlackSync.routing.SpaceTokenInfo
import org.jetbrains.spaceSlackSync.slack.SlackClient
import org.jetbrains.spaceSlackSync.storage.SlackTeamCached

class SlackChannelsToPickForSync(spaceTokenInfo: SpaceTokenInfo) : ServiceBase(spaceTokenInfo) {
    suspend fun getSlackChannelsToPickFrom(slackTeamId: String, query: String): SlackChannelsToPickForSyncResponse {
        val alreadySyncedChannelIds = db.syncedChannels.getBySlackTeamId(slackTeamId).map { it.slackChannelId }.toSet()

        val slackClient = SlackClient.tryCreate(slackTeamId) ?: return SlackChannelsToPickForSyncResponse.Empty

        val channelsToPickForSync = SlackTeamCache.getSlackTeam(slackClient, slackTeamId)
            .toSlackChannelsToPickForSync(alreadySyncedChannelIds, query)
        return SlackChannelsToPickForSyncResponse(channelsToPickForSync)
    }

    private fun SlackTeamCached.toSlackChannelsToPickForSync(alreadySyncedChannelIds: Set<String>, query: String) =
        this.channels
            .filter { it.id !in alreadySyncedChannelIds }
            .filter { it.name.lowercase().contains(query.lowercase()) }
            .take(BATCH_SIZE)
            .map { SlackChannelToPickForSync(it.name, it.id) }
}

@Serializable
data class SlackChannelsToPickForSyncResponse(
    val slackChannelsToPickForSync: List<SlackChannelToPickForSync>,
) {
    companion object {
        val Empty = SlackChannelsToPickForSyncResponse(emptyList())
    }
}

@Serializable
data class SlackChannelToPickForSync(
    val channelNameInSlack: String,
    val slackChannelId: String,
)

private const val BATCH_SIZE = 20
