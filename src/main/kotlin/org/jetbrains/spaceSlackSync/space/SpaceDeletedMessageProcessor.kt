package org.jetbrains.spaceSlackSync.space

import org.jetbrains.spaceSlackSync.db
import org.jetbrains.spaceSlackSync.slack.SlackClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.jetbrains.spaceSlackSync.storage.SyncedChannel
import java.lang.invoke.MethodHandles

suspend fun processDeletedChatMessageFromSpace(
    syncedChannel: SyncedChannel,
    spaceMessageId: String
) {
    log.debug("ARCHIVED message from Space. Space client id: ${syncedChannel.spaceAppClientId}, space channel id: ${syncedChannel.spaceChannelId}")

    val slackClient = SlackClient.tryCreate(syncedChannel.slackTeamId) ?: return
    val slackMessageId = db.messages.getSlackMsgBySpaceMsg(spaceMessageId) ?: return
    slackClient.deleteMessage(syncedChannel.slackChannelId, slackMessageId)
}

private val log: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
