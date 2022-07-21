package org.jetbrains.spaceSlackSync.space

import org.jetbrains.spaceSlackSync.db
import org.jetbrains.spaceSlackSync.slack.SlackClient
import org.jetbrains.spaceSlackSync.storage.SyncedChannel

suspend fun processDeletedChatMessageFromSpace(
    syncedChannel: SyncedChannel,
    spaceMessageId: String
) {
    val slackClient = SlackClient.tryCreate(syncedChannel.slackTeamId) ?: return
    val slackMessageId = db.messages.getSlackMsgBySpaceMsg(spaceMessageId) ?: return
    slackClient.deleteMessage(syncedChannel.slackChannelId, slackMessageId)
}
