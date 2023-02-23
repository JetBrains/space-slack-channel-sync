package org.jetbrains.spaceSlackSync.space

import org.jetbrains.spaceSlackSync.db
import org.jetbrains.spaceSlackSync.slack.SlackClient
import org.jetbrains.spaceSlackSync.withSyncedChannelLogContext
import org.jetbrains.spaceSlackSync.withSpaceMessageLogContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.helpers.ProcessingScope
import space.jetbrains.api.runtime.types.*
import java.lang.invoke.MethodHandles

suspend fun ProcessingScope.processChatMessageFromSpace(event: WebhookEvent) {
    val spaceChatEventData = event.spaceChatEventData()
    if (spaceChatEventData == null) {
        log.warn("Ignored webhook event ${event::class.qualifiedName}")
        return
    }

    withSpaceMessageLogContext(spaceChatEventData) {
        try {
            doProcessChatMessageFromSpace(event, spaceChatEventData)
        } catch (e: Exception) {
            log.error("Error while processing chat message webhook request from Space", e)
        }
    }
}

private suspend fun ProcessingScope.doProcessChatMessageFromSpace(
    event: WebhookEvent,
    spaceChatEventData: SpaceChatEventData
) {
    when (event) {
        is ChatMessageCreatedEvent -> {
            log.debug("NEW message from Space")
            val context = getContext(spaceChatEventData) ?: return // reason is logged inside
            withSyncedChannelLogContext(context.syncedChannel) {
                context.processNewChatMessageFromSpace()
            }
        }

        is ChatMessageUpdatedEvent -> {
            log.debug("UPDATED message from Space")
            val context = getContext(spaceChatEventData) ?: return // reason is logged inside
            withSyncedChannelLogContext(context.syncedChannel) {
                context.processUpdatedChatMessageFromSpace()
            }
        }

        is ChatMessageDeletedEvent -> {
            log.debug("ARCHIVED message from Space")
            val syncedChannel = getSyncedChannel(spaceChatEventData)
            if (syncedChannel == null) {
                log.debug("SKIP the Space message deletion event: synced channel record not found by Space client id and Space channel id")
            } else {
                withSyncedChannelLogContext(syncedChannel) {
                    processDeletedChatMessageFromSpace(syncedChannel, event.message.id)
                }
            }
        }

        else -> return
    }
}

private fun WebhookEvent.spaceChatEventData() = when (this) {
    is ChatMessageCreatedEvent -> SpaceChatEventData(channelId, message.id, threadId)
    is ChatMessageUpdatedEvent -> SpaceChatEventData(channelId, message.id, threadId)
    is ChatMessageDeletedEvent -> SpaceChatEventData(channelId, message.id, threadId)
    else -> null
}

private suspend fun ProcessingScope.getContext(spaceChatEventData: SpaceChatEventData): MessageFromSpaceCtx? {
    val spaceClient = clientWithClientCredentials()
    val message = getMessage(spaceClient, spaceChatEventData)

    if (!message.externalId.isNullOrEmpty()) {
        log.debug("SKIP the Space message: it has been imported into Space and shouldn't be propagated")
        return null
    }
    if (message.details !is M2TextItemContent) {
        log.debug("SKIP the Space message: messages from applications are not yet supported")
        return null
    }

    val syncedChannel = getSyncedChannel(spaceChatEventData)
    if (syncedChannel == null) {
        log.debug("SKIP the Space message: synced channel record not found by Space client id and Space channel id")
        return null
    }

    return withSyncedChannelLogContext(syncedChannel) {
        val slackClient = SlackClient.tryCreate(syncedChannel.slackTeamId)
        if (slackClient == null) {
            log.debug("SKIP the Space message: could not create Slack client")
            return@withSyncedChannelLogContext null
        }

        MessageFromSpaceCtx(spaceClient, slackClient, syncedChannel, message, spaceChatEventData)
    }
}

private suspend fun ProcessingScope.getSyncedChannel(spaceChatEventData: SpaceChatEventData) =
    db.syncedChannels.getByAppClientId(spaceAppClientId = appInstance.clientId)
        .firstOrNull { it.spaceChannelId == spaceChatEventData.spaceChannelId }

private val log: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
