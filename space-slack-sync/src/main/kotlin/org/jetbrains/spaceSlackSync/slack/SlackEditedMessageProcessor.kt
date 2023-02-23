package org.jetbrains.spaceSlackSync.slack

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.resources.chats
import space.jetbrains.api.runtime.types.ChatMessageIdentifier
import space.jetbrains.api.runtime.types.ImportMessage
import java.lang.invoke.MethodHandles

suspend fun MessageFromSlackCtx.processEditedMessage(event: SlackMessageEvent.MessageUpdated) {
    val (messageAuthor, messageContent) = messageAuthorAndContent(event)

    val channel = channelIdentifier(event) ?: return // reason is logged inside
    spaceClient.chats.messages.importMessages(
        channel = channel,
        messages = listOf(
            ImportMessage.Update(
                messageId = ChatMessageIdentifier.ExternalId(event.messageId),
                message = messageContent,
                author = messageAuthor,
                editedAtUtc = event.editedTs.toUtcLong(),
                attachments = getAttachments(event)
            )
        )
    )

    log.debug("Message updated in Space")
}

private val log: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
