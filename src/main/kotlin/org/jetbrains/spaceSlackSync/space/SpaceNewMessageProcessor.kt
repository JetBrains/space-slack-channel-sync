package org.jetbrains.spaceSlackSync.space

import org.jetbrains.spaceSlackSync.db
import space.jetbrains.api.runtime.types.CUserPrincipalDetails
import space.jetbrains.api.runtime.types.TD_MemberProfile

suspend fun MessageFromSpaceCtx.processNewChatMessageFromSpace() {
    val spaceProfile = (message.author.details as? CUserPrincipalDetails)?.user
    val slackUser = matchSlackUserByEmails(slackClient, spaceProfile)
    val slackThreadId = getSlackThreadId()

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

    response?.message?.ts?.let { slackMessageTs ->
        db.messages.setSlackMsgBySpaceMsg(syncedChannel.slackTeamId, slackMessageTs, message.id)
    }
}
