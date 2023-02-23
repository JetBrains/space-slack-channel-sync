package org.jetbrains.spaceSlackSync.homepage

import kotlinx.serialization.Serializable
import org.jetbrains.spaceSlackSync.db
import org.jetbrains.spaceSlackSync.routing.SpaceTokenInfo
import space.jetbrains.api.runtime.BatchInfo
import space.jetbrains.api.runtime.resources.chats

class SpaceChannelsToPickForSync(spaceTokenInfo: SpaceTokenInfo) : ServiceBase(spaceTokenInfo) {
    suspend fun getSpaceChannelsToPickFrom(query: String): SpaceChannelsToPickForSyncResponse {
        val storedSyncedChannelIds = db.syncedChannels
            .getByAppClientId(spaceTokenInfo.spaceAppInstance.clientId)
            .map { it.spaceChannelId }
            .toSet()

        val serverUrl = spaceTokenInfo.spaceAppInstance.spaceServer.serverUrl
        val spaceChannelsToPickForSync = userSpaceClient.chats.channels.listAllChannels(
            query = query,
            withArchived = false,
            publicOnly = true,
            batchInfo = BatchInfo("0", 40)
        ) {
            name()
            icon()
            channelId()
            access()
        }.data
            .map { SpaceChannelToPickForSync(it.name, it.channelId, it.icon?.let { icon -> "$serverUrl/d/$icon" }) }
            .filter { it.spaceChannelId !in storedSyncedChannelIds }

        return SpaceChannelsToPickForSyncResponse(spaceChannelsToPickForSync)
    }
}

@Serializable
data class SpaceChannelsToPickForSyncResponse(
    val spaceChannelsToPickForSync: List<SpaceChannelToPickForSync>,
)

@Serializable
data class SpaceChannelToPickForSync(
    val channelNameInSpace: String,
    val spaceChannelId: String,
    val icon: String?,
)
