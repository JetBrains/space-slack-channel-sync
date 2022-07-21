@file:Suppress("MoveVariableDeclarationIntoWhen")

package org.jetbrains.spaceSlackSync.slack

import com.slack.api.app_backend.events.payload.MessagePayload
import com.slack.api.model.BotProfile
import com.slack.api.model.File
import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.event.MessageEvent
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.spaceSlackSync.gson

sealed interface SlackMessageEvent {
    val teamId: String
    val channelId: String
    val messageId: String
    val threadId: String?

    class MessageCreated(
        override val teamId: String,
        override val channelId: String,
        override val messageId: String,
        override val threadId: String?,
        override val files: List<File>?,
        override val blocks: List<LayoutBlock>?,
        override val text: String?,
        override val botProfile: BotProfile?,
        override val userId: String?,
    ) : MessageCreatedOrUpdated, SlackMessageEvent

    class MessageUpdated(
        override val teamId: String,
        override val channelId: String,
        override val messageId: String,
        override val threadId: String?,
        override val files: List<File>?,
        override val blocks: List<LayoutBlock>?,
        override val text: String?,
        override val botProfile: BotProfile?,
        override val userId: String?,
        val editedTs: String,
    ) : MessageCreatedOrUpdated, SlackMessageEvent

    class MessageDeleted(
        override val teamId: String,
        override val channelId: String,
        override val messageId: String,
        override val threadId: String?,
    ) : SlackMessageEvent

    class ChannelJoin(
        override val teamId: String,
        override val channelId: String,
        override val messageId: String,
        val joinedUserId: String,
        val invitedById: String?,
    ) : SlackMessageEvent {
        override val threadId: String? = null
    }
}

interface MessageCreatedOrUpdated {
    val teamId: String
    val channelId: String
    val messageId: String
    val threadId: String?
    val files: List<File>?
    val blocks: List<LayoutBlock>?
    val text: String?
    val botProfile: BotProfile?
    val userId: String?
}

fun getSlackMessageEvent(requestBodyJson: JsonElement, requestBody: String): SlackMessageEvent? {
    val type = getSlackMessageType(requestBodyJson) ?: return null
    return when (type) {
        SlackMessageType.NEW_MESSAGE -> {
            val message = gson.fromJson(requestBody, MessagePayload::class.java)
            val messageEvent = message.event
            SlackMessageEvent.MessageCreated(
                channelId = messageEvent.channel,
                teamId = messageEvent.team ?: message.teamId,
                messageId = messageEvent.ts,
                threadId = messageEvent.threadTs,
                files = messageEvent.files,
                blocks = messageEvent.blocks,
                text = messageEvent.text,
                botProfile = messageEvent.botProfile,
                userId = messageEvent.user
            )
        }

        SlackMessageType.MESSAGE_EDITED -> {
            val message = gson.fromJson(requestBody, MessagePayload::class.java)
            val messageEvent = message.event
            val eventJson = requestBodyJson.jsonObject["event"] ?: return null
            val newMessageJson = eventJson.jsonObject["message"] ?: return null
            val previousMessageJson = eventJson.jsonObject["previous_message"] ?: return null
            val previousMessage = gson.fromJson(previousMessageJson.toString(), MessageEvent::class.java)
            val newMessage = gson.fromJson(newMessageJson.toString(), MessageEvent::class.java)

            SlackMessageEvent.MessageUpdated(
                channelId = messageEvent.channel,
                teamId = newMessage.team ?: previousMessage.team ?: message.teamId,
                messageId = previousMessage.ts,
                threadId = previousMessage.threadTs,
                files = newMessage.files,
                blocks = newMessage.blocks,
                text = newMessage.text,
                botProfile = newMessage.botProfile,
                userId = newMessage.user,
                editedTs = newMessage.ts,
            )
        }

        SlackMessageType.MESSAGE_DELETED -> {
            val message = gson.fromJson(requestBody, MessagePayload::class.java)
            val messageEvent = message.event
            val eventJson = requestBodyJson.jsonObject["event"] ?: return null
            val previousMessageJson = eventJson.jsonObject["previous_message"] ?: return null
            val previousMessage = gson.fromJson(previousMessageJson.toString(), MessageEvent::class.java)
            SlackMessageEvent.MessageDeleted(
                channelId = messageEvent.channel,
                teamId = previousMessage.team ?: message.teamId,
                messageId = previousMessage.ts,
                threadId = previousMessage.threadTs
            )
        }

        SlackMessageType.CHANNEL_JOIN -> {
            val message = gson.fromJson(requestBody, MessagePayload::class.java)
            val messageEvent = message.event
            val eventJson = requestBodyJson.jsonObject["event"] ?: return null
            val invitedById = eventJson.jsonObject["inviter"]?.jsonPrimitive?.content
            SlackMessageEvent.ChannelJoin(
                teamId = message.teamId,
                channelId = messageEvent.channel,
                messageId = messageEvent.ts,
                joinedUserId = messageEvent.user,
                invitedById = invitedById
            )
        }
    }
}

enum class SlackMessageType {
    NEW_MESSAGE,
    MESSAGE_EDITED,
    MESSAGE_DELETED,
    CHANNEL_JOIN
}

fun getSlackMessageType(requestBodyJson: JsonElement): SlackMessageType? {
    val eventJson = requestBodyJson.jsonObject["event"] ?: return null
    val messageJson = eventJson.jsonObject["message"]
    val messageSubtype = messageJson?.jsonObject?.get("subtype")?.jsonPrimitive?.content
    val eventSubtype = eventJson.jsonObject["subtype"]?.jsonPrimitive?.content
    return when (eventSubtype) {
        "message_deleted" -> SlackMessageType.MESSAGE_DELETED
        "message_changed" -> {
            when (messageSubtype) {
                "tombstone" -> SlackMessageType.MESSAGE_DELETED
                else -> SlackMessageType.MESSAGE_EDITED
            }
        }
        "channel_join" -> return SlackMessageType.CHANNEL_JOIN
        else -> SlackMessageType.NEW_MESSAGE
    }
}
