@file:Suppress("MoveVariableDeclarationIntoWhen")

package org.jetbrains.spaceSlackSync.slack

import com.slack.api.app_backend.events.payload.MessagePayload
import com.slack.api.model.BotProfile
import com.slack.api.model.Field
import com.slack.api.model.File
import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.event.MessageEvent
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.spaceSlackSync.gson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

sealed interface SlackMessageEvent {
    val teamId: String
    val channelId: String
    val messageId: String
    val threadId: String?
    val userId: String?

    class MessageCreated(
        override val teamId: String,
        override val channelId: String,
        override val messageId: String,
        override val threadId: String?,
        override val files: List<File>?,
        override val blocks: List<LayoutBlock>,
        override val fields: List<Field>,
        override val text: String?,
        override val botProfile: BotProfile?,
        override val userId: String?,
        override val color: String?,
    ) : MessageCreatedOrUpdated, SlackMessageEvent

    class MessageUpdated(
        override val teamId: String,
        override val channelId: String,
        override val messageId: String,
        override val threadId: String?,
        override val files: List<File>?,
        override val blocks: List<LayoutBlock>,
        override val fields: List<Field>,
        override val text: String?,
        override val botProfile: BotProfile?,
        override val userId: String?,
        override val color: String?,
        val editedTs: String,
    ) : MessageCreatedOrUpdated, SlackMessageEvent

    class MessageDeleted(
        override val teamId: String,
        override val channelId: String,
        override val messageId: String,
        override val threadId: String?,
        override val userId: String?,
    ) : SlackMessageEvent

    class ChannelJoin(
        override val teamId: String,
        override val channelId: String,
        override val messageId: String,
        val joinedUserId: String,
        val invitedById: String?,
    ) : SlackMessageEvent {
        override val userId = joinedUserId
        override val threadId: String? = null
    }

    class ChannelLeave(
        override val teamId: String,
        override val channelId: String,
        override val messageId: String,
        val leftUserId: String,
    ) : SlackMessageEvent {
        override val threadId: String? = null
        override val userId = leftUserId
    }
}

interface MessageCreatedOrUpdated {
    val teamId: String
    val channelId: String
    val messageId: String
    val threadId: String?
    val files: List<File>?
    val blocks: List<LayoutBlock>
    val fields: List<Field>
    val text: String?
    val botProfile: BotProfile?
    val userId: String?
    val color: String?
}

fun getSlackMessageEvent(requestBodyJson: JsonElement, requestBody: String): SlackMessageEvent? {
    val eventJson = requestBodyJson.jsonObject["event"]
    if (eventJson == null) {
        log.debug("SKIP Slack message: `event` JSON field is not found")
        return null
    }

    val type = getSlackMessageType(eventJson)

    return when (type) {
        SlackMessageType.NEW_MESSAGE -> {
            val message = gson.fromJson(requestBody, MessagePayload::class.java)
            val messageEvent = message.event
            val attachmentBlocks = message.event.attachments.orEmpty().flatMap { it.blocks.orEmpty() }
            val fields = message.event.attachments.orEmpty().flatMap { it.fields.orEmpty() }
            val color = message.event.attachments.orEmpty().firstOrNull()?.color
            SlackMessageEvent.MessageCreated(
                channelId = messageEvent.channel,
                teamId = messageEvent.team ?: message.teamId,
                messageId = messageEvent.ts,
                threadId = messageEvent.threadTs,
                files = messageEvent.files,
                blocks = messageEvent.blocks.orEmpty() + attachmentBlocks,
                fields = fields,
                text = messageEvent.text,
                botProfile = messageEvent.botProfile,
                userId = messageEvent.user,
                color = color,
            )
        }

        SlackMessageType.MESSAGE_EDITED -> {
            val message = gson.fromJson(requestBody, MessagePayload::class.java)
            val messageEvent = message.event
            val newMessageJson = eventJson.jsonObject["message"]
            if (newMessageJson == null) {
                log.debug("SKIP Slack message: `event -> message` JSON field not found for ${SlackMessageType.MESSAGE_EDITED} message type")
                return null
            }

            val previousMessageJson = eventJson.jsonObject["previous_message"]
            if (previousMessageJson == null) {
                log.debug("SKIP Slack message: `event -> previous_message` JSON field not found for ${SlackMessageType.MESSAGE_EDITED} message type")
                return null
            }

            val previousMessage = gson.fromJson(previousMessageJson.toString(), MessageEvent::class.java)
            val newMessage = gson.fromJson(newMessageJson.toString(), MessageEvent::class.java)
            val fields = newMessage.attachments.orEmpty().flatMap { it.fields.orEmpty() }
            val attachmentBlocks = newMessage.attachments.orEmpty().flatMap { it.blocks.orEmpty() }
            val color = newMessage.attachments.orEmpty().firstOrNull()?.color

            SlackMessageEvent.MessageUpdated(
                channelId = messageEvent.channel,
                teamId = newMessage.team ?: previousMessage.team ?: message.teamId,
                messageId = previousMessage.ts,
                threadId = previousMessage.threadTs,
                files = newMessage.files,
                blocks = newMessage.blocks.orEmpty() + attachmentBlocks,
                fields = fields,
                text = newMessage.text,
                botProfile = newMessage.botProfile,
                userId = newMessage.user,
                editedTs = newMessage.ts,
                color = color,
            )
        }

        SlackMessageType.MESSAGE_DELETED -> {
            val message = gson.fromJson(requestBody, MessagePayload::class.java)
            val messageEvent = message.event

            val previousMessageJson = eventJson.jsonObject["previous_message"]
            if (previousMessageJson == null) {
                log.debug("SKIP Slack message: `event -> previous_message` JSON field not found for ${SlackMessageType.MESSAGE_DELETED} message type")
                return null
            }

            val previousMessage = gson.fromJson(previousMessageJson.toString(), MessageEvent::class.java)
            SlackMessageEvent.MessageDeleted(
                channelId = messageEvent.channel,
                teamId = previousMessage.team ?: message.teamId,
                messageId = previousMessage.ts,
                threadId = previousMessage.threadTs,
                userId = previousMessage.user,
            )
        }

        SlackMessageType.USER_JOINED_CHANNEL -> {
            val message = gson.fromJson(requestBody, MessagePayload::class.java)
            val messageEvent = message.event
            val invitedById = eventJson.jsonObject["inviter"]?.jsonPrimitive?.content
            SlackMessageEvent.ChannelJoin(
                teamId = message.teamId,
                channelId = messageEvent.channel,
                messageId = messageEvent.ts,
                joinedUserId = messageEvent.user,
                invitedById = invitedById
            )
        }

        SlackMessageType.USER_LEFT_CHANNEL -> {
            val message = gson.fromJson(requestBody, MessagePayload::class.java)
            val messageEvent = message.event
            SlackMessageEvent.ChannelLeave(
                teamId = message.teamId,
                channelId = messageEvent.channel,
                messageId = messageEvent.ts,
                leftUserId = messageEvent.user,
            )
        }
    }
}

enum class SlackMessageType {
    NEW_MESSAGE,
    MESSAGE_EDITED,
    MESSAGE_DELETED,
    USER_JOINED_CHANNEL,
    USER_LEFT_CHANNEL,
}

private fun getSlackMessageType(eventJson: JsonElement): SlackMessageType {
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
        "channel_join" -> SlackMessageType.USER_JOINED_CHANNEL
        "channel_leave" -> SlackMessageType.USER_LEFT_CHANNEL
        else -> SlackMessageType.NEW_MESSAGE
    }
}

private val log: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
