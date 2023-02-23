package org.jetbrains.spaceSlackSync.slack

import org.jetbrains.spaceSlackSync.db
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.resources.chats
import space.jetbrains.api.runtime.types.ChatMessageIdentifier
import space.jetbrains.api.runtime.types.ImportMessage
import java.lang.invoke.MethodHandles

suspend fun MessageFromSlackCtx.processDeletedMessage(event: SlackMessageEvent.MessageDeleted) {
    val channel = channelIdentifier(event) ?: return // reason is logged inside

    val messageInfo = db.messages.getInfoBySlackMsg(syncedChannel.slackTeamId, event.messageId)
    if (messageInfo?.deleted == true) {
        log.debug("SKIP propagation of Slack message deletion to Space: message is already marked as deleted")
        return
    }

    spaceClient.chats.messages.importMessages(
        channel = channel,
        messages = listOf(
            ImportMessage.Delete(
                ChatMessageIdentifier.ExternalId(event.messageId)
            )
        )
    )

    db.messages.markAsDeletedBySlackMessageId(syncedChannel.slackTeamId, slackMessageId = event.messageId)

    log.debug("Message deleted in Space")
}

private val log: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
