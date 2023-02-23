package org.jetbrains.spaceSlackSync.slack

import org.jetbrains.spaceSlackSync.db
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.resources.chats
import space.jetbrains.api.runtime.types.ChannelIdentifier
import space.jetbrains.api.runtime.types.ChannelItemRecord
import space.jetbrains.api.runtime.types.ChatMessageIdentifier
import space.jetbrains.api.runtime.types.TD_MemberProfile
import java.lang.invoke.MethodHandles

class SlackUserData(
    val id: String,
    val nameToUseInMessage: String?,
    val spaceProfile: TD_MemberProfile?,
)

suspend fun MessageFromSlackCtx.channelIdentifier(event: SlackMessageEvent): ChannelIdentifier? {
    val ts = event.messageId
    val threadTs = event.threadId

    return if (threadTs != null && threadTs != ts) {
        val spaceMessageId = db.messages.getInfoBySlackMsg(syncedChannel.slackTeamId, threadTs)?.spaceMessageId
            ?: tryGetSpaceMessageByExternalId(threadTs, syncedChannel.spaceChannelId)?.id

        if (spaceMessageId != null) {
            ChannelIdentifier.Thread(spaceMessageId)
        } else {
            log.debug("SKIP Slack message: cannot identify Space channel to post to")
            return null
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

private val log: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
