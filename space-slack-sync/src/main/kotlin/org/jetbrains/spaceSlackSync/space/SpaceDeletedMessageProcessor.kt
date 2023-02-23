package org.jetbrains.spaceSlackSync.space

import org.jetbrains.spaceSlackSync.db
import org.jetbrains.spaceSlackSync.slack.SlackClient
import org.jetbrains.spaceSlackSync.storage.SyncedChannel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

suspend fun processDeletedChatMessageFromSpace(
    syncedChannel: SyncedChannel,
    spaceMessageId: String
) {
    val slackClient = SlackClient.tryCreate(syncedChannel.slackTeamId) ?: return  // reason is logged inside
    val messageInfo = db.messages.getInfoBySpaceMsg(syncedChannel.slackTeamId, spaceMessageId)
    if (messageInfo == null) {
        log.debug("SKIP propagation of Space message deletion to Slack: message record not found")
        return
    }

    if (messageInfo.deleted) {
        log.debug("SKIP propagation of Space message deletion to Slack: message is already marked as deleted")
        return
    }

    slackClient.deleteMessage(syncedChannel.slackChannelId, messageInfo.slackMessageId)

    db.messages.markAsDeletedBySpaceMessageId(syncedChannel.slackTeamId, spaceMessageId = spaceMessageId)

    log.debug("Message deleted in Slack")
}

private val log: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
