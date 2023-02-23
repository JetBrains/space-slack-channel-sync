package org.jetbrains.spaceSlackSync.slack

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.resources.chats
import space.jetbrains.api.runtime.types.*
import java.lang.invoke.MethodHandles

suspend fun MessageFromSlackCtx.processChannelJoin(event: SlackMessageEvent.ChannelJoin) {
    if (event.invitedById == null) {
        // Only send join messages to Space when there is an invitee. This mirrors Space logic for
        // joining a channel.
        log.debug("SKIP Slack message: channel join message has no invitee")
        return
    }

    val slackProfile = slackClient.getUserById(event.joinedUserId)
    if (slackProfile == null) {
        log.debug("SKIP Slack message: cannot retrieve Slack user who joined the channel")
        return
    }

    val spaceUser = getSpaceTdMemberProfile(SlackPrincipal.SlackUser(slackProfile), "user who joined the channel")

    val messageAuthor = if (spaceUser != null) {
        PrincipalIn.Profile(ProfileIdentifier.Id(spaceUser.id))
    } else {
        PrincipalIn.Application(ApplicationIdentifier.Me)
    }

    val spaceProfileText = if (spaceUser != null) {
        "@${spaceUser.username}"
    } else {
        slackProfile.realName?.takeIf { it.isNotEmpty() } ?: slackProfile.displayName ?: ""
    }

    val invitedByProfile = event.invitedById?.let { slackClient.getUserById(it) }
    val invitedBySpaceUser = invitedByProfile?.let { getSpaceTdMemberProfile(SlackPrincipal.SlackUser(invitedByProfile), "user who invited another user to join the channel") }

    val inviteeSpaceProfileText = if (invitedBySpaceUser != null) {
        "@${invitedBySpaceUser.username}"
    } else {
        invitedByProfile?.realName
    }

    val slackChannel = slackClient.getChannelInfo(syncedChannel.slackChannelId)
    if (slackChannel == null) {
        log.debug("SKIP Slack message: cannot retrieve Slack channel that was joined")
        return
    }

    val channelLink = "[#${slackChannel.name}](https://slack.com/app_redirect?channel=${syncedChannel.slackChannelId})"
    val messageText = if (inviteeSpaceProfileText == null) {
        "$spaceProfileText joined $channelLink channel in Slack"
    } else {
        val actionText = if (slackProfile.botId?.isNotEmpty() == true) {
            "added"
        } else {
            "invited"
        }
        "$spaceProfileText was $actionText by $inviteeSpaceProfileText to $channelLink channel in Slack"
    }

    val channel = channelIdentifier(event) ?: return // reason is logged inside

    spaceClient.chats.messages.importMessages(
        channel = channel,
        messages = listOf(
            ImportMessage.Create(
                messageId = ChatMessageIdentifier.ExternalId(event.messageId),
                message = ChatMessage.Text(messageText),
                author = messageAuthor,
                createdAtUtc = event.messageId.toUtcLong(),
                editedAtUtc = null,
                attachments = emptyList()
            )
        ),
        suppressNotifications = true
    )

    log.debug("Channel-join message imported into Space")
}

private val log: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
