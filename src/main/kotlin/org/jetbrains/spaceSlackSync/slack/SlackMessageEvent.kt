package org.jetbrains.spaceSlackSync.slack

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.spaceSlackSync.SlackCredentials
import org.jetbrains.spaceSlackSync.db
import org.jetbrains.spaceSlackSync.homepage.spaceHttpClient
import org.jetbrains.spaceSlackSync.storage.SlackTeam
import org.jetbrains.spaceSlackSync.storage.SyncedChannel
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.SpaceAuth
import space.jetbrains.api.runtime.SpaceClient

class MessageFromSlackCtx(
    val slackClient: SlackClient,
    val spaceClient: SpaceClient,
    val syncedChannel: SyncedChannel,
    val slackTeam: SlackTeam,
)

suspend fun processMessageEvent(requestBodyJson: JsonElement, requestBody: String) {
    if (isMessagePostedByThisApp(requestBodyJson)) {
        return
    }
    val slackMessageEvent = getSlackMessageEvent(requestBodyJson, requestBody) ?: return
    val context = getContext(slackMessageEvent) ?: return

    when (slackMessageEvent) {
        is SlackMessageEvent.MessageCreated -> {
            context.processNewMessage(slackMessageEvent)
        }
        is SlackMessageEvent.MessageUpdated -> {
            context.processEditedMessage(slackMessageEvent)
        }
        is SlackMessageEvent.MessageDeleted -> {
            context.processDeletedMessage(slackMessageEvent)
        }
        is SlackMessageEvent.ChannelJoin -> {
            context.processChannelJoin(slackMessageEvent)
        }
    }
}

private suspend fun getContext(slackMessageEvent: SlackMessageEvent): MessageFromSlackCtx? {
    val syncedChannel =
        db.syncedChannels.getBySlackChannelId(slackMessageEvent.teamId, slackMessageEvent.channelId) ?: return null
    val spaceAppInstance =
        db.spaceAppInstances.getById(syncedChannel.spaceAppClientId, slackMessageEvent.teamId) ?: return null
    val spaceClient = SpaceClient(spaceHttpClient, spaceAppInstance, SpaceAuth.ClientCredentials())
    val slackClient = SlackClient.tryCreate(slackMessageEvent.teamId) ?: return null
    val slackTeam = db.slackTeams.getById(slackMessageEvent.teamId) ?: return null

    return MessageFromSlackCtx(slackClient, spaceClient, syncedChannel, slackTeam)
}

private fun isMessagePostedByThisApp(requestBodyJson: JsonElement): Boolean {
    val eventJson = requestBodyJson.jsonObject["event"]
    val newMessageJson = eventJson?.jsonObject?.get("message")
    val appId = eventJson?.jsonObject?.get("app_id")?.jsonPrimitive?.content
        ?: newMessageJson?.jsonObject?.get("app_id")?.jsonPrimitive?.content

    return appId == SlackCredentials.appId
}

private val log = LoggerFactory.getLogger("SlackMessageEvent")
