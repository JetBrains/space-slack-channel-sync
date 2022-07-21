package org.jetbrains.spaceSlackSync.slack

import org.jetbrains.spaceSlackSync.db
import space.jetbrains.api.runtime.resources.chats
import space.jetbrains.api.runtime.types.ChannelIdentifier
import space.jetbrains.api.runtime.types.ChannelItemRecord
import space.jetbrains.api.runtime.types.ChatMessageIdentifier
import space.jetbrains.api.runtime.types.TD_MemberProfile

class SlackUserData(
    val id: String,
    val nameToUseInMessage: String?,
    val spaceProfile: TD_MemberProfile?,
)

suspend fun MessageFromSlackCtx.channelIdentifier(event: SlackMessageEvent): ChannelIdentifier {
    val ts = event.messageId
    val threadTs = event.threadId

    return if (threadTs != null && threadTs != ts) {
        val spaceMessageId = db.messages.getSpaceMsgBySlackMsg(threadTs)
            ?: tryGetSpaceMessageByExternalId(threadTs, syncedChannel.spaceChannelId)?.id

        if (spaceMessageId != null) {
            ChannelIdentifier.Thread(spaceMessageId)
        } else {
            ChannelIdentifier.Id(syncedChannel.spaceChannelId)
        }
    } else {
        ChannelIdentifier.Id(syncedChannel.spaceChannelId)
    }
}

private suspend fun MessageFromSlackCtx.tryGetSpaceMessageByExternalId(
    threadTs: String,
    spaceChannelId: String
): ChannelItemRecord? {
    return try {
        spaceClient.chats.messages.getMessage(
            message = ChatMessageIdentifier.ExternalId(threadTs),
            channel = ChannelIdentifier.Id(spaceChannelId),
        )
    } catch (e: Exception) {
        null
    }
}

fun String.toUtcLong() = this.replace(".", "").toLong() / 1000