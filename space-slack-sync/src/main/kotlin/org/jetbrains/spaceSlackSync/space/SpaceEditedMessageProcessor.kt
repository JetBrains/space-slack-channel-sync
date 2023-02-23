package org.jetbrains.spaceSlackSync.space

import org.jetbrains.spaceSlackSync.db
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

suspend fun MessageFromSpaceCtx.processUpdatedChatMessageFromSpace() {
    val messageText = buildMessageTextForSlack(spaceClient, slackClient, message.text)
    val spaceAttachments = getAttachments()

    val slackMessageId = db.messages.getInfoBySpaceMsg(syncedChannel.slackTeamId, eventData.spaceMessageId)?.slackMessageId ?: run {
        log.warn("Edit Space message with id ${eventData.spaceMessageId}: couldn't find the message in DB")
        return
    }

    slackClient.editMessage {
        it.channel(syncedChannel.slackChannelId)
        it.text(messageText)
        it.blocks(slackMessageBlocks(messageText, spaceAttachments))
        it.ts(slackMessageId)
    }

    log.debug("Message updated in Slack")
}

private val log: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
