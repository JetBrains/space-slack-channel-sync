package org.jetbrains.spaceSlackSync.slack

import org.jetbrains.spaceSlackSync.db
import space.jetbrains.api.runtime.resources.chats
import space.jetbrains.api.runtime.types.ChatMessageIdentifier
import space.jetbrains.api.runtime.types.ImportMessage

suspend fun MessageFromSlackCtx.processNewMessage(event: SlackMessageEvent.MessageCreated) {
    val (messageAuthor, messageContent) = messageAuthorAndContent(event)

    val channel = channelIdentifier(event) ?: return
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

    // TODO: make it possible to identify thread in Space by ExternalId. Currently, we need the internal id for that.
    //       also, we cannot just request the internal id by external every time, because the thread root message may be deleted
    val spaceMessageId = spaceClient.chats.messages.getMessage(ChatMessageIdentifier.ExternalId(event.messageId), channel).id
    db.messages.setSlackMsgBySpaceMsg(event.teamId, event.messageId, spaceMessageId)
}
