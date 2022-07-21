package org.jetbrains.spaceSlackSync.slack

import space.jetbrains.api.runtime.resources.chats
import space.jetbrains.api.runtime.types.*

suspend fun MessageFromSlackCtx.processChannelJoin(event: SlackMessageEvent.ChannelJoin) {
    val profile = slackClient.getUserById(event.joinedUserId) ?: return
    val spaceUserId = getSpaceUserId(SlackPrincipal.SlackUser(profile))

    val messageAuthor = if (spaceUserId != null) {
        PrincipalIn.Profile(ProfileIdentifier.Id(spaceUserId))
    } else {
        PrincipalIn.Application(ApplicationIdentifier.Me)
    }

    val spaceProfileText = if (spaceUserId != null) {
        "@{$spaceUserId,,,}"
    } else {
        profile.realName?.takeIf { it.isNotEmpty() } ?: profile.displayName ?: ""
    }

    val invitedByProfile = event.invitedById?.let { slackClient.getUserById(it) }
    val invitedBySpaceUser = invitedByProfile?.let { getSpaceUserId(SlackPrincipal.SlackUser(invitedByProfile)) }

    val inviteeSpaceProfileText = if (invitedBySpaceUser != null) {
        "@{$invitedBySpaceUser,,,}"
    } else {
        invitedByProfile?.realName
    }

    val slackChannel = slackClient.getChannelInfo(syncedChannel.slackChannelId) ?: return

    val channelLink = "[#${slackChannel.name}](https://slack.com/app_redirect?channel=${syncedChannel.slackChannelId})"
    val messageText = if (inviteeSpaceProfileText == null) {
        "$spaceProfileText joined $channelLink channel in Slack"
    } else {
        val actionText = if (profile.botId?.isNotEmpty() == true) {
            "added"
        } else {
            "invited"
        }
        "$spaceProfileText was $actionText by $inviteeSpaceProfileText to $channelLink channel in Slack"
    }

    spaceClient.chats.messages.importMessages(
        channel = channelIdentifier(event),
        messages = listOf(
            ImportMessage.Create(
                messageId = ChatMessageIdentifier.ExternalId(event.messageId),
                message = ChatMessage.Text(messageText),
                author = messageAuthor,
                createdAtUtc = event.messageId.toUtcLong(),
                editedAtUtc = null,
                attachments = emptyList()
            )
        )
    )
}
