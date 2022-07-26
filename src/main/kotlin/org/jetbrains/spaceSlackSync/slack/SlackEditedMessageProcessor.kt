package org.jetbrains.spaceSlackSync.slack

import space.jetbrains.api.runtime.resources.chats
import space.jetbrains.api.runtime.types.ChatMessageIdentifier
import space.jetbrains.api.runtime.types.ImportMessage

suspend fun MessageFromSlackCtx.processEditedMessage(event: SlackMessageEvent.MessageUpdated) {
    val (messageAuthor, messageContent) = messageAuthorAndContent(event)

    val channel = channelIdentifier(event) ?: return
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
}
