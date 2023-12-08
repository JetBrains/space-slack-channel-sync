package org.jetbrains.spaceSlackSync.homepage

import kotlinx.serialization.Serializable
import org.jetbrains.spaceSlackSync.db
import org.jetbrains.spaceSlackSync.routing.SpaceTokenInfo

class StopSyncService(spaceTokenInfo: SpaceTokenInfo) : ServiceBase(spaceTokenInfo) {
    suspend fun stopSync(spaceChannelId: String, slackTeamId: String, slackChannelId: String): StopSyncResponse {
        db.syncedChannels.getByIds(
            spaceTokenInfo.spaceAppInstance.clientId,
            spaceChannelId,
            slackTeamId,
            slackChannelId
        )?.let {
//            if (!userHasAdminPermission(spaceChannelId)) {
//                return StopSyncResponse(
//                    success = false,
//                    "Only a channel administrator can stop synchronization for that channel"
//                )
//            }

            db.syncedChannels.remove(
                spaceTokenInfo.spaceAppInstance.clientId,
                spaceChannelId,
                slackTeamId,
                slackChannelId
            )
            return StopSyncResponse(success = true)
        }
        return StopSyncResponse(success = false, message = "Could not find the synced channel to remove")
    }
}

@Serializable
class StopSyncResponse(
    val success: Boolean,
    val message: String? = null
)
