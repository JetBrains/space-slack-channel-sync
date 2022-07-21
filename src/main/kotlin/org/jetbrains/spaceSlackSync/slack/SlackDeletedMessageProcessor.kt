package org.jetbrains.spaceSlackSync.slack

import space.jetbrains.api.runtime.resources.chats
import space.jetbrains.api.runtime.types.ChatMessageIdentifier
import space.jetbrains.api.runtime.types.ImportMessage

suspend fun MessageFromSlackCtx.processDeletedMessage(event: SlackMessageEvent.MessageDeleted) {
    spaceClient.chats.messages.importMessages(
        channel = channelIdentifier(event),
        messages = listOf(
            ImportMessage.Delete(
                ChatMessageIdentifier.ExternalId(event.messageId)
            )
        )
    )
}
