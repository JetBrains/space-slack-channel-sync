package org.jetbrains.spaceSlackSync.slack

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.spaceSlackSync.SlackCredentials
import org.jetbrains.spaceSlackSync.db
import org.jetbrains.spaceSlackSync.homepage.spaceHttpClient
import org.jetbrains.spaceSlackSync.storage.SlackTeam
import org.jetbrains.spaceSlackSync.storage.SyncedChannel
import org.jetbrains.spaceSlackSync.withSyncedChannelLogContext
import org.jetbrains.spaceSlackSync.withSlackEventLogContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.SpaceAuth
import space.jetbrains.api.runtime.SpaceClient
import java.lang.invoke.MethodHandles

class MessageFromSlackCtx(
    val slackClient: SlackClient,
    val spaceClient: SpaceClient,
    val syncedChannel: SyncedChannel,
    val slackTeam: SlackTeam,
)

suspend fun processMessageEvent(requestBodyJson: JsonElement, requestBody: String) {
    if (isMessagePostedByThisApp(requestBodyJson)) {
        log.debug("SKIP message from Slack: Message was posted by this app")
        return
    }

    val slackMessageEvent = getSlackMessageEvent(requestBodyJson, requestBody) ?: return // reason is logged inside
    withSlackEventLogContext(slackMessageEvent) {
        val context = getContext(slackMessageEvent) ?: return@withSlackEventLogContext // reason is logged inside

        withSyncedChannelLogContext(context.syncedChannel) {
            when (slackMessageEvent) {
                is SlackMessageEvent.MessageCreated -> {
                    log.debug("NEW message from Slack")
                    context.processNewMessage(slackMessageEvent)
                }

                is SlackMessageEvent.MessageUpdated -> {
                    log.debug("UPDATED message from Slack")
                    context.processEditedMessage(slackMessageEvent)
                }

                is SlackMessageEvent.MessageDeleted -> {
                    log.debug("DELETED message from Slack")
                    context.processDeletedMessage(slackMessageEvent)
                }

                is SlackMessageEvent.ChannelJoin -> {
                    log.debug("CHANNEL JOIN message from Slack")
                    context.processChannelJoin(slackMessageEvent)
                }

                is SlackMessageEvent.ChannelLeave -> {
                    log.debug("CHANNEL LEAVE message from Slack")
                    context.processChannelLeave(slackMessageEvent)
                }
            }
        }
    }
}

private suspend fun getContext(slackMessageEvent: SlackMessageEvent): MessageFromSlackCtx? {
    val syncedChannel = db.syncedChannels.getBySlackChannel(slackMessageEvent.teamId, slackMessageEvent.channelId)
    if (syncedChannel == null) {
        log.debug("SKIP Slack message: synced channel record not found by teamId and channelId")
        return null
    }

    return withSyncedChannelLogContext(syncedChannel) {
        getMessageFromSlackCtx(slackMessageEvent, syncedChannel)
    }
}

private suspend fun getMessageFromSlackCtx(
    slackMessageEvent: SlackMessageEvent,
    syncedChannel: SyncedChannel
): MessageFromSlackCtx? {
    val spaceAppInstance = db.spaceAppInstances.getById(syncedChannel.spaceAppClientId, slackMessageEvent.teamId)
    if (spaceAppInstance == null) {
        log.debug("SKIP Slack message: Space app instance not found by spaceAppClientId and teamId")
        return null
    }

    val spaceClient = SpaceClient(spaceHttpClient, spaceAppInstance, SpaceAuth.ClientCredentials())
    val slackTeam = db.slackTeams.getById(slackMessageEvent.teamId)
    if (slackTeam == null) {
        log.debug("SKIP Slack message: Slack team record not found by slack team id")
        return null
    }
    val slackClient = slackAppClient(slackTeam)

    return MessageFromSlackCtx(slackClient, spaceClient, syncedChannel, slackTeam)
}

private fun isMessagePostedByThisApp(requestBodyJson: JsonElement): Boolean {
    val eventJson = requestBodyJson.jsonObject["event"]
    val newMessageJson = eventJson?.jsonObject?.get("message")
    val appId = eventJson?.jsonObject?.get("app_id")?.jsonPrimitive?.content
        ?: newMessageJson?.jsonObject?.get("app_id")?.jsonPrimitive?.content

    return appId == SlackCredentials.appId
}

private val log: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
