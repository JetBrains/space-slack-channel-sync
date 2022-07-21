package org.jetbrains.spaceSlackSync.space

import org.jetbrains.spaceSlackSync.db
import org.jetbrains.spaceSlackSync.slack.SlackClient
import space.jetbrains.api.runtime.helpers.ProcessingScope
import space.jetbrains.api.runtime.types.*

suspend fun ProcessingScope.processChatMessageFromSpace(event: WebhookEvent) {
    when (event) {
        is ChatMessageCreatedEvent -> {
            val spaceChatEventData = SpaceChatEventData(event.channelId, event.message.id, event.threadId)
            val context = getContext(spaceChatEventData) ?: return
            context.processNewChatMessageFromSpace()
        }
        is ChatMessageUpdatedEvent -> {
            val spaceChatEventData = SpaceChatEventData(event.channelId, event.message.id, event.threadId)
            val context = getContext(spaceChatEventData) ?: return
            context.processUpdatedChatMessageFromSpace()
        }
        is ChatMessageDeletedEvent -> {
            val spaceChatEventData = SpaceChatEventData(event.channelId, event.message.id, event.threadId)
            val syncedChannel = getSyncedChannel(spaceChatEventData) ?: return
            processDeletedChatMessageFromSpace(syncedChannel, event.message.id)
        }
        else -> return
    }
}

private suspend fun ProcessingScope.getContext(spaceChatEventData: SpaceChatEventData): MessageFromSpaceCtx? {
    val spaceClient = clientWithClientCredentials()
    val message = getMessage(spaceClient, spaceChatEventData)

    if (!message.externalId.isNullOrEmpty()) {
        // this message has been imported into Space and shouldn't be propagated
        return null
    }
    if (message.details !is M2TextItemContent) {
        // this is a message that was not created by user manually
        return null
    }

    val syncedChannel = getSyncedChannel(spaceChatEventData) ?: return null
    val slackClient = SlackClient.tryCreate(syncedChannel.slackTeamId) ?: return null

    return MessageFromSpaceCtx(spaceClient, slackClient, syncedChannel, message, spaceChatEventData)
}

private suspend fun ProcessingScope.getSyncedChannel(spaceChatEventData: SpaceChatEventData) =
    db.syncedChannels.getByAppClientId(spaceAppClientId = appInstance.clientId)
        .firstOrNull { it.spaceChannelId == spaceChatEventData.spaceChannelId }
