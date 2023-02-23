package org.jetbrains.spaceSlackSync.homepage

import io.ktor.client.plugins.*
import io.ktor.server.config.*
import kotlinx.serialization.Serializable
import org.jetbrains.spaceSlackSync.config
import org.jetbrains.spaceSlackSync.db
import org.jetbrains.spaceSlackSync.routing.SpaceTokenInfo
import org.jetbrains.spaceSlackSync.slack.SlackClient
import org.jetbrains.spaceSlackSync.storage.SyncedChannel
import space.jetbrains.api.runtime.ktorClientForSpace
import space.jetbrains.api.runtime.resources.chats
import space.jetbrains.api.runtime.resources.permissions
import space.jetbrains.api.runtime.types.*

val spaceHttpClient = ktorClientForSpace {
    install(HttpTimeout) {
        connectTimeoutMillis =
            config.tryGetString("space.httpClient.connectTimeoutMs")?.toLong() ?: DEFAULT_CONNECT_TIMEOUT_MS
        socketTimeoutMillis =
            config.tryGetString("space.httpClient.socketTimeoutMs")?.toLong() ?: DEFAULT_SOCKET_TIMEOUT_MS
        requestTimeoutMillis =
            config.tryGetString("space.httpClient.requestTimeoutMs")?.toLong() ?: DEFAULT_REQUEST_TIMEOUT_MS
    }
}

private const val DEFAULT_CONNECT_TIMEOUT_MS = 60_000L
private const val DEFAULT_SOCKET_TIMEOUT_MS = 60_000L
private const val DEFAULT_REQUEST_TIMEOUT_MS = 60_000L

class SyncedChannelsService(spaceTokenInfo: SpaceTokenInfo) : ServiceBase(spaceTokenInfo) {
    suspend fun listSyncedChannels(): ListSyncedChannelsResponse {
        val storedSyncedChannels = db.syncedChannels.getByAppClientId(spaceTokenInfo.spaceAppInstance.clientId)
        return ListSyncedChannelsResponse(
            syncedChannels = storedSyncedChannels.mapNotNull { it.toInfo() }
        )
    }

    private suspend fun SyncedChannel.toInfo(): SyncedChannelInfo? {
        // TODO: get channel names in batches
        val (channelNameInSpace, iconId) = tryGetChannelName(ChannelIdentifier.Id(this.spaceChannelId)) ?: return null

        val slackClient = SlackClient.tryCreate(this.slackTeamId) ?: return null
        val slackChannel = slackClient.getChannelInfo(slackChannelId) ?: return null

        val serverUrl = spaceTokenInfo.spaceAppInstance.spaceServer.serverUrl
        val iconUrl = iconId?.let { "$serverUrl/d/$iconId" }

        return SyncedChannelInfo(
            channelNameInSpace = channelNameInSpace,
            channelNameInSlack = slackChannel.name,
            spaceChannelId = this.spaceChannelId,
            spaceChannelIconUrl = iconUrl,
            slackTeamId = this.slackTeamId,
            slackChannelId = this.slackChannelId,
            isMemberInSlackChannel = slackChannel.isMember,
            isAuthorizedInSpaceChannel = appHasPostMessagesPermission(this.spaceChannelId), // TODO: check permissions in batches (add API method in Space)
            userIsAdminInSpaceChannel = userHasAdminPermission(this.spaceChannelId), // TODO: check permissions in batches (add API method in Space)
        )
    }

    private suspend fun appHasPostMessagesPermission(spaceChannelId: String): Boolean {
        return appSpaceClient.permissions.checkPermission(
            PrincipalIn.Application(ApplicationIdentifier.Me),
            PermissionIdentifier.ImportMessages,
            ChannelPermissionTarget(ChannelIdentifier.Id(spaceChannelId))
        )
    }

    private suspend fun tryGetChannelName(channelIdentifier: ChannelIdentifier): Pair<String, String?>? {
        return try {
            val content = userSpaceClient.chats.channels.getChannel(channelIdentifier) {
                content {
                    name()
                    iconId()
                }
            }.content

            (content as? M2SharedChannelContent)?.let {
                it.name to it.iconId
            }
        } catch (e: Exception) {
            null
        }
    }
}

@Serializable
data class ListSyncedChannelsResponse(
    val syncedChannels: List<SyncedChannelInfo>,
)

@Serializable
data class SyncedChannelInfo(
    val channelNameInSpace: String,
    val channelNameInSlack: String,
    val spaceChannelId: String,
    val spaceChannelIconUrl: String?,
    val slackTeamId: String,
    val slackChannelId: String,
    val isMemberInSlackChannel: Boolean,
    val isAuthorizedInSpaceChannel: Boolean,
    val userIsAdminInSpaceChannel: Boolean,
    val isNewChannelForOptimisticUpdate: Boolean = false,
)
