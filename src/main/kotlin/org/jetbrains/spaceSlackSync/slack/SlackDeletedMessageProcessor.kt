package org.jetbrains.spaceSlackSync.slack

import space.jetbrains.api.runtime.resources.chats
import space.jetbrains.api.runtime.types.ChatMessageIdentifier
import space.jetbrains.api.runtime.types.ImportMessage

suspend fun MessageFromSlackCtx.processDeletedMessage(event: SlackMessageEvent.MessageDeleted) {
    val channel = channelIdentifier(event) ?: return
    spaceClient.chats.messages.importMessages(
        channel = channel,
        messages = listOf(
            ImportMessage.Delete(
                ChatMessageIdentifier.ExternalId(event.messageId)
            )
        )
    )
}
