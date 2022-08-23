package org.jetbrains.spaceSlackSync.slack

import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.block.RichTextBlock
import com.slack.api.model.block.element.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter


suspend fun StringBuilder.buildMessageText(
    slackClient: SlackClient,
    slackDomain: String,
    layoutBlocks: List<LayoutBlock>?,
    messageText: String?,
    slackUserDataById: Map<String, SlackUserData>
) {
    layoutBlocks?.filterIsInstance<RichTextBlock>()
        ?.takeUnless { it.isEmpty() }
        ?.flatMap { it.elements }
        ?.filterIsInstance<RichTextElement>()
        ?.forEach { appendRichTextElement(it, slackClient, slackDomain, slackUserDataById) }
        ?: run {
            append(messageText)
        }
}

private suspend fun StringBuilder.appendRichTextElement(
    element: RichTextElement,
    slackClient: SlackClient,
    slackDomain: String,
    slackUserDataById: Map<String, SlackUserData>
) {
    when (element) {
        is RichTextSectionElement -> {
            element.elements.forEach { appendRichTextElement(it, slackClient, slackDomain, slackUserDataById) }
        }
        is RichTextListElement -> {
            element.elements.forEach { listItem ->
                repeat(element.indent) {
                    append("   ")
                }
                append(if (element.style == "ordered") "1. " else "* ")
                appendRichTextElement(listItem, slackClient, slackDomain, slackUserDataById)
                appendLine()
            }
            appendLine()
        }
        is RichTextPreformattedElement -> {
            appendLine("```")
            element.elements.forEach { appendRichTextElement(it, slackClient, slackDomain, slackUserDataById) }
            appendLine()
            appendLine("```")
        }
        is RichTextQuoteElement -> {
            element.elements.forEach {
                append("> ")
                appendRichTextElement(it, slackClient, slackDomain, slackUserDataById)
                appendLine()
                appendLine()
            }
        }
        is RichTextSectionElement.Text -> {
            appendStyled(element.style, element.text)
        }
        is RichTextSectionElement.Channel -> {
            slackClient.getChannelInfo(element.channelId)?.channelLink(slackClient, slackDomain, element.channelId)
                ?.let {
                    appendStyled(element.style, it)
                }
        }
        is RichTextSectionElement.User -> {
            slackUserDataById.get(element.userId)?.let { slackUserData ->
                slackUserData.spaceProfile?.let { spaceProfile ->
                    appendStyled(element.style, "@${spaceProfile.username}")
                } ?: run {
                    appendStyled(element.style, "`@${slackUserData.nameToUseInMessage}`")
                }
            }
        }
        is RichTextSectionElement.Link -> {
            if (element.text.isNullOrBlank())
                appendStyled(element.style, element.url)
            else
                appendStyled(element.style, "[${element.text}](${element.url})")
        }
        is RichTextSectionElement.Team -> {
            slackClient.getTeamInfo { it.team(element.teamId) }?.let {
                appendStyled(element.style, "`@${it.team.name}`")
            }
        }
        is RichTextSectionElement.UserGroup -> {
            slackClient.getUserGroups { it }?.usergroups
                ?.firstOrNull { it.id == element.usergroupId }
                ?.let {
                    append(it.name)
                }
        }
        is RichTextSectionElement.Date -> {
            element.timestamp?.toLongOrNull()?.let {
                Instant.fromEpochSeconds(it)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .toJavaLocalDateTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            }
        }
        is RichTextSectionElement.Broadcast -> {
            append("`@${element.range}`")
        }
        is RichTextSectionElement.Emoji -> {
            append(":${element.name}:")
        }
    }
}

private fun StringBuilder.appendStyled(style: RichTextSectionElement.TextStyle?, text: String) {
    text.takeWhile { it.isWhitespace() }.takeUnless { it.isEmpty() }?.let { append(it) }
    putStyleMarker(style, true)
    append(text.trim())
    putStyleMarker(style, false)
    text.takeLastWhile { it.isWhitespace() }.takeUnless { it.isEmpty() }?.let { append(it) }
}

private fun StringBuilder.putStyleMarker(style: RichTextSectionElement.TextStyle?, pre: Boolean) {
    if (style != null) {
        val markers = listOfNotNull(
            "**".takeIf { style.isBold },
            "_".takeIf { style.isItalic },
            "~~".takeIf { style.isStrike },
            "`".takeIf { style.isCode }
        )
        if (markers.isNotEmpty()) {
            if (pre)
                append(markers.joinToString(""))
            else
                append(markers.reversed().joinToString(""))
        }
    }
}

fun getMentionedSlackUserIds(layoutBlocks: List<LayoutBlock>?): Set<String> {
    val userIds = mutableSetOf<String>()
    layoutBlocks?.filterIsInstance<RichTextBlock>()
        ?.takeUnless { it.isEmpty() }
        ?.flatMap { it.elements }
        ?.filterIsInstance<RichTextElement>()
        ?.forEach { userIds.addSlackUserIds(it) }

    return userIds
}

private fun MutableSet<String>.addSlackUserIds(element: RichTextElement) {
    when (element) {
        is RichTextSectionElement -> {
            element.elements.forEach { addSlackUserIds(it) }
        }
        is RichTextListElement -> {
            element.elements.forEach { addSlackUserIds(it) }
        }
        is RichTextPreformattedElement -> {
            element.elements.forEach { addSlackUserIds(it) }
        }
        is RichTextQuoteElement -> {
            element.elements.forEach { addSlackUserIds(it) }
        }
        is RichTextSectionElement.User -> {
            add(element.userId)
        }
    }
}
