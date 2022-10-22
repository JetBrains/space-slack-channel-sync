package org.jetbrains.spaceSlackSync.space

import com.slack.api.model.User
import org.jetbrains.spaceSlackSync.slack.SlackClient
import space.jetbrains.api.runtime.SpaceClient
import space.jetbrains.api.runtime.resources.richText
import space.jetbrains.api.runtime.resources.teamDirectory
import space.jetbrains.api.runtime.types.*

suspend fun buildMessageTextForSlack(
    spaceClient: SpaceClient,
    slackClient: SlackClient,
    spaceMessageText: String
): String {
    return buildString {
        val spaceRichText = spaceClient.richText.parseMarkdown(spaceMessageText)
        val spaceUserIds = gatherSpaceUserIds(spaceRichText)
        // TODO: add API method to avoid N+1
        val spaceProfiles = spaceUserIds.map { spaceUserId ->
            spaceClient.teamDirectory.profiles.getProfile(ProfileIdentifier.Id(spaceUserId)) {
                id()
                emails {
                    email()
                    blocked()
                }
            }
        }

        val slackUserBySpaceUserId =
            spaceProfiles.mapNotNull { profile ->
                matchSlackUserByEmails(slackClient, profile)?.let { slackUser ->
                    profile.id to slackUser
                }
            }.toMap()

        appendDocument(spaceRichText, slackUserBySpaceUserId)
    }
}

fun gatherSpaceUserIds(spaceRichText: RtDocument): Set<String> {
    val spaceUserIds = mutableSetOf<String>()
    spaceUserIds.gatherUserIds(spaceRichText)
    return spaceUserIds
}

private fun StringBuilder.appendDocument(doc: RtDocument, slackUserBySpaceUserId: Map<String, User>) {
    doc.children.forEach { appendBlockNode(it, linePrefix = "", slackUserBySpaceUserId) }
}

private fun StringBuilder.appendBlockNode(
    node: BlockNode,
    linePrefix: String,
    slackUserBySpaceUserId: Map<String, User>,
    prefixForFirstLine: Boolean = false
) {
    fun appendWithLinePrefix(s: String, ix: Int) =
        append("${if (ix > 0 || prefixForFirstLine) linePrefix else ""}$s")

    when (node) {
        is RtBlockquote -> {
            node.children.forEachIndexed { ix, child ->
                appendWithLinePrefix("> ", ix)
                appendBlockNode(child, "$linePrefix\t", slackUserBySpaceUserId)
            }
            if (linePrefix.isEmpty())
                appendLine()
        }
        is RtBulletList -> {
            if (linePrefix.isEmpty())
                appendLine()
            node.children.forEachIndexed { ix, child ->
                appendWithLinePrefix("*  ", ix)
                appendBlockNode(child, "$linePrefix\t", slackUserBySpaceUserId)
            }
            if (linePrefix.isEmpty())
                appendLine()
        }
        is RtOrderedList -> {
            if (linePrefix.isEmpty())
                appendLine()
            node.children.forEachIndexed { ix, item ->
                appendWithLinePrefix("${ix + node.startNumber}.  ", ix)
                appendBlockNode(item, "$linePrefix\t", slackUserBySpaceUserId)
            }
            if (linePrefix.isEmpty())
                appendLine()
        }
        is RtListItem -> {
            node.children.forEachIndexed { ix, child ->
                appendBlockNode(child, linePrefix, slackUserBySpaceUserId, prefixForFirstLine || ix > 0)
            }
        }
        is RtCode -> {
            appendLine("```")
            node.children.forEach {
                appendInlineNode(it, slackUserBySpaceUserId)
                appendLine()
            }
            appendLine("```")
            appendLine()
        }
        is RtHeading -> {
            if (prefixForFirstLine) {
                append(linePrefix)
            }
            node.children.forEach { appendInlineNode(it, slackUserBySpaceUserId) }
            appendLine()
        }
        is RtParagraph -> {
            if (prefixForFirstLine) {
                append(linePrefix)
            }
            node.children.forEach { appendInlineNode(it, slackUserBySpaceUserId) }
            appendLine()
        }
    }
}

private fun StringBuilder.appendInlineNode(node: InlineNode, slackUserBySpaceUserId: Map<String, User>) {
    when (node) {
        is RtBreak ->
            appendLine()
        is RtImage -> {}
        is RtText -> {
            val slackUser = node.marks.firstNotNullOfOrNull {
                it.getSpaceUserId()?.let { spaceUserId -> slackUserBySpaceUserId[spaceUserId] }
            }
            node.marks.forEach { openMark(it) }

            if (slackUser != null) {
                append(slackUser.name)
            } else {
                append(node.value)
            }

            node.marks.forEach { closeMark(it) }
        }
        is RtMention -> {
            val slackUser = node.getSpaceProfileId()?.let { spaceUserId -> slackUserBySpaceUserId[spaceUserId] }
            if (slackUser != null) {
                append("@${slackUser.name}")
            } else {
//                append("[${node.plainTextValue()}](${node.attrs.href})")
                append("@${node.plainTextValue()}")
            }
        }
        is RtEmoji -> {
            append(":${node.emojiName}:")
        }
    }
}

private fun StringBuilder.openMark(mark: DocumentMark) {
    when (mark) {
        is RtStrikeThroughMark ->
            append("~")
        is RtLinkMark ->
            if (mark.attrs.details.let { it is RtTeamLinkDetails || it is RtProfileLinkDetails || it is RtPredefinedMentionLinkDetails })
                append("@")
            else
                append("<${mark.attrs.href}|")
        is RtItalicMark ->
            append("_")
        is RtCodeMark ->
            append("`")
        is RtBoldMark ->
            append("*")
    }
}

private fun StringBuilder.closeMark(mark: DocumentMark) {
    when (mark) {
        is RtStrikeThroughMark ->
            append("~")
        is RtLinkMark ->
            if (!mark.attrs.details.let { it is RtTeamLinkDetails || it is RtProfileLinkDetails || it is RtPredefinedMentionLinkDetails })
                append(">")
        is RtItalicMark ->
            append("_")
        is RtCodeMark ->
            append("`")
        is RtBoldMark ->
            append("*")
    }
}

private fun MutableSet<String>.gatherUserIds(spaceRichTextDoc: RtDocument) {
    spaceRichTextDoc.children.forEach { gatherUserIds(it) }
}

private fun MutableSet<String>.gatherUserIds(node: BlockNode) {
    when (node) {
        is RtBlockquote -> {
            node.children.forEach { gatherUserIds(it) }
        }
        is RtBulletList -> {
            node.children.forEach { gatherUserIds(it) }
        }
        is RtOrderedList -> {
            node.children.forEach { gatherUserIds(it) }
        }
        is RtListItem -> {
            node.children.forEach { gatherUserIds(it) }
        }
        is RtCode -> {
            node.children.forEach { gatherUserIds(it) }
        }
        is RtHeading -> {
            node.children.forEach { gatherUserIds(it) }
        }
        is RtParagraph -> {
            node.children.forEach { gatherUserIds(it) }
        }
    }
}

private fun DocumentMark.getSpaceUserId(): String? {
    when (this) {
        is RtLinkMark -> {
            val details = attrs.details
            if (details is RtProfileLinkDetails) {
                return details.id
            }
        }
    }
    return null
}

private fun MutableSet<String>.gatherUserIds(node: InlineNode) {
    when (node) {
        is RtText -> {
            node.marks.forEach { mark ->
                mark.getSpaceUserId()?.let { add(it) }
            }
        }
        is RtMention -> {
            node.getSpaceProfileId()?.let { add(it) }
        }
    }
}

private fun RtMention.getSpaceProfileId(): String? {
    return when (val mentionAttrs = attrs) {
        is RtProfileMentionAttrs -> {
            mentionAttrs.id
        }
        is RtTeamMentionAttrs -> {
            null
        }
        is RtPredefinedMentionAttrs -> {
            null
        }
        else -> null
    }
}

private fun RtMention.plainTextValue(): String {
    return when (val mentionAttrs = attrs) {
        is RtProfileMentionAttrs -> {
            mentionAttrs.userName
        }
        is RtTeamMentionAttrs -> {
            mentionAttrs.teamName
        }
        is RtPredefinedMentionAttrs -> {
            ""
        }
        else -> ""
    }
}
