package org.jetbrains.spaceSlackSync.slack

import org.jetbrains.spaceSlackSync.db
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.resources.chats
import space.jetbrains.api.runtime.types.ChatMessageIdentifier
import space.jetbrains.api.runtime.types.ImportMessage
import java.lang.invoke.MethodHandles

suspend fun MessageFromSlackCtx.processNewMessage(event: SlackMessageEvent.MessageCreated) {
    val (messageAuthor, messageContent) = messageAuthorAndContent(event)

    val channel = channelIdentifier(event) ?: return // reason is logged inside

    log.trace("Start importMessage call")
    spaceClient.chats.messages.importMessages(
        channel = channel,
        messages = listOf(
            ImportMessage.Create(
                messageId = ChatMessageIdentifier.ExternalId(event.messageId),
                message = messageContent,
                author = messageAuthor,
                createdAtUtc = event.messageId.toUtcLong(),
                editedAtUtc = null,
                attachments = getAttachments(event)
            )
        )
    )
    log.trace("Finish importMessage call")

    // TODO: make it possible to identify thread in Space by ExternalId. Currently, we need the internal id for that.
    //       also, we cannot just request the internal id by external every time, because the thread root message may be deleted
    val spaceMessageId = spaceClient.chats.messages.getMessage(ChatMessageIdentifier.ExternalId(event.messageId), channel).id
    db.messages.setSlackMsgBySpaceMsg(event.teamId, event.messageId, spaceMessageId)

    log.debug("Message info saved into DB")
}

private val log: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
