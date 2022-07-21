package org.jetbrains.spaceSlackSync.space

import com.slack.api.model.block.composition.MarkdownTextObject
import com.slack.api.model.kotlin_extension.block.FileSource
import com.slack.api.model.kotlin_extension.block.withBlocks
import io.ktor.server.config.*
import org.jetbrains.spaceSlackSync.config
import org.jetbrains.spaceSlackSync.db
import org.jetbrains.spaceSlackSync.slack.SlackClient
import org.jetbrains.spaceSlackSync.storage.SyncedChannel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.SpaceClient
import space.jetbrains.api.runtime.resources.chats
import space.jetbrains.api.runtime.resources.uploads
import space.jetbrains.api.runtime.types.*
import java.net.URI

class MessageFromSpaceCtx(
    val spaceClient: SpaceClient,
    val slackClient: SlackClient,
    val syncedChannel: SyncedChannel,
    val message: ChannelItemRecord,
    val eventData: SpaceChatEventData,
)

class SpaceChatEventData(
    val spaceChannelId: String,
    val spaceMessageId: String,
    val spaceThreadId: String?,
)

sealed class SpaceAttachment {
    class ImageAttachment(val publicImageUrl: String, val name: String) : SpaceAttachment()

    // includes video attachments
    class FileAttachment(val publicFileUrl: String, val slackExternalId: String) : SpaceAttachment()
}

suspend fun getMessage(
    spaceClient: SpaceClient,
    spaceChatEventData: SpaceChatEventData,
): ChannelItemRecord {
    val channelIdentifier = getChannelIdentifier(spaceChatEventData)
    return spaceClient.chats.messages.getMessage(
        message = ChatMessageIdentifier.InternalId(spaceChatEventData.spaceMessageId),
        channel = channelIdentifier,
    ) {
        details()
        author {
            details {
                user {
                    name {
                        firstName()
                        lastName()
                    }
                    emails {
                        email()
                        blocked()
                    }
                }
            }
        }
        text()
        externalId()

        thread {
            id()
        }

        id()
        attachments()
    }
}

suspend fun MessageFromSpaceCtx.getAttachments(): List<SpaceAttachment> {
    return message.attachments.orEmpty()
        .mapNotNull { it.details }
        .mapNotNull { attachment ->
            when (attachment) {
                is ImageAttachment -> SpaceAttachment.ImageAttachment(
                    getPublicUrl(attachment.id),
                    attachment.name ?: ""
                )
                is VideoAttachment -> {
                    val videoFilePublicUrl = getPublicUrl(attachment.id)
                    slackClient.addRemoteFile(
                        externalId = attachment.id,
                        filePublicUrl = videoFilePublicUrl,
                        title = attachment.name ?: "video"
                    )
                    SpaceAttachment.FileAttachment(videoFilePublicUrl, attachment.id)
                }
                is FileAttachment -> {
                    val filePublicUrl = getPublicUrl(attachment.id)
                    slackClient.addRemoteFile(
                        externalId = attachment.id,
                        filePublicUrl = filePublicUrl,
                        title = attachment.filename
                    )
                    SpaceAttachment.FileAttachment(filePublicUrl, attachment.id)
                }
                else -> null
            }
        }
}

suspend fun MessageFromSpaceCtx.getSlackThreadId(): String? {
    return eventData.spaceThreadId?.let { threadId ->
        val channel = spaceClient.chats.channels.getChannel(ChannelIdentifier.Id(threadId)) {
            content {
                record {
                    id()
                    externalId()
                }
            }
        }

        val spaceThreadStartMsg = (channel.content as? M2ChannelContentThread)?.record
        spaceThreadStartMsg?.externalId ?: spaceThreadStartMsg?.id?.let { threadStartMessageId ->
            db.messages.getSlackMsgBySpaceMsg(threadStartMessageId)
        }
    }
}

suspend fun matchSlackUserByEmails(slackClient: SlackClient, spaceProfile: TD_MemberProfile?) =
    spaceProfile.userEmails()
        .firstNotNullOfOrNull { spaceUserEmail ->
            slackClient.tryLookupUserByEmail(spaceUserEmail)
        }

fun slackMessageBlocks(
    messageText: String,
    spaceAttachments: List<SpaceAttachment>
) = withBlocks {
    section {
        text(MarkdownTextObject.TYPE, messageText)
    }
    spaceAttachments.forEach { spaceAttachment ->
        when (spaceAttachment) {
            is SpaceAttachment.ImageAttachment -> {
                image {
                    title(spaceAttachment.name)
                    imageUrl(spaceAttachment.publicImageUrl)
                    altText("")
                }
            }
            is SpaceAttachment.FileAttachment -> {
                file(externalId = spaceAttachment.slackExternalId, source = FileSource.REMOTE)
            }
        }
    }
}

private fun TD_MemberProfile?.userEmails(): List<String> {
    this ?: return emptyList()
    return this.emails
        .filter { it.blocked != true }
        .map { it.email }
}


private suspend fun MessageFromSpaceCtx.getPublicUrl(attachmentId: String): String {
    val attachmentUrl = spaceClient.uploads.chat.publicUrl.getPublicUrl(
        ChannelIdentifier.Id(syncedChannel.spaceChannelId),
        ChatMessageIdentifier.InternalId(message.id),
        attachmentId
    )
    return urlWithSubstitutedHost(attachmentUrl)
}

private fun urlWithSubstitutedHost(urlString: String): String {
    val spaceHostForTesting = config.tryGetString("app.spacePublicUrl") ?: return urlString
    val uri = URI(urlString)
    return "$spaceHostForTesting${uri.path}"
}

private fun getChannelIdentifier(spaceChatEventData: SpaceChatEventData) =
    spaceChatEventData.spaceThreadId?.let {
        ChannelIdentifier.Id(it)
    } ?: ChannelIdentifier.Id(spaceChatEventData.spaceChannelId)

private val log: Logger = LoggerFactory.getLogger("SpaceMessageProcessor")
