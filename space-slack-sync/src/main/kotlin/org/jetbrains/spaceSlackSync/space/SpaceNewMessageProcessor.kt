package org.jetbrains.spaceSlackSync.space

import org.jetbrains.spaceSlackSync.db
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.types.CUserPrincipalDetails
import java.lang.invoke.MethodHandles

suspend fun MessageFromSpaceCtx.processNewChatMessageFromSpace() {
    val spaceProfile = (message.author.details as? CUserPrincipalDetails)?.user
    val slackUser = matchSlackUserByEmails(slackClient, spaceProfile)
    val slackThreadId = getSlackThreadId()
    if (eventData.spaceThreadId != null && slackThreadId == null) {
        log.debug("Processing new message from Space. Thread not found in Slack, thread root should be an old message that was not synced")
        return
    }

    val messageText = buildMessageTextForSlack(spaceClient, slackClient, message.text)
    val spaceAttachments = getAttachments()

    val response = slackClient.postMessage {
        it.channel(syncedChannel.slackChannelId)
        it.text(messageText)
        it.blocks(slackMessageBlocks(messageText, spaceAttachments))

        slackThreadId?.let { slackThreadId -> it.threadTs(slackThreadId) }

        slackUser?.let { slackUser ->
            it.username(slackUser.profile.realName)
            it.iconUrl(slackUser.profile.image48)
        } ?: run {
            if (spaceProfile != null) {
                it.username("${spaceProfile.name.firstName} ${spaceProfile.name.lastName}")
            } else {
                it.username(message.author.name)
            }
        }
    }

    log.debug("New message posted to Slack")

    response?.message?.ts?.let { slackMessageTs ->
        db.messages.setSlackMsgBySpaceMsg(syncedChannel.slackTeamId, slackMessageTs, message.id)
        log.debug("Message info saved to DB")
    }
}

private val log: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
