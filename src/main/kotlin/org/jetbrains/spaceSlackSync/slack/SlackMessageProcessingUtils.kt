package org.jetbrains.spaceSlackSync.slack

import com.slack.api.model.BotProfile
import com.slack.api.model.User
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.resources.teamDirectory
import space.jetbrains.api.runtime.resources.uploads
import space.jetbrains.api.runtime.types.*
import java.lang.invoke.MethodHandles

suspend fun MessageFromSlackCtx.getAttachments(event: MessageCreatedOrUpdated): List<AttachmentIn> {
    return event.files.orEmpty()
        .map {
            val url = it.urlPrivateDownload
            val uploadUrl = spaceClient.uploads.createUpload("spaceSlackChannelSync")
            val serverUrl = spaceClient.server.serverUrl
            val token = spaceClient.auth.token(spaceClient.ktorClient, spaceClient.appInstance)

            // TODO: download and upload by chunk
            val downloadedBytes = slackClient.downloadFile(url)
            val attachmentId: String = spaceClient.ktorClient.request("$serverUrl$uploadUrl/${it.id}") {
                method = HttpMethod.Put
                setBody(ByteArrayContent(downloadedBytes))
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()

            when (it.filetype.lowercase()) {
                in imageFileExtensions -> ImageAttachment(
                    attachmentId,
                    it.name,
                    it.originalWidth.toIntOrNull() ?: 0,
                    it.originalHeight.toIntOrNull() ?: 0,
                    null,
                    null
                )
                else -> FileAttachment(attachmentId, downloadedBytes.size.toLong(), it.name)
            }
        }
}

suspend fun MessageFromSlackCtx.messageAuthorAndContent(event: MessageCreatedOrUpdated): Pair<PrincipalIn, ChatMessage> {
    val slackPrincipal = getSlackPrincipal(event)
    val spaceUser = getSpaceUserId(slackPrincipal)

    return when {
        spaceUser != null -> PrincipalIn.Profile(ProfileIdentifier.Id(spaceUser.id)) to spaceMessage(event)
        else -> {
            val principalName = when (slackPrincipal) {
                is SlackPrincipal.SlackUser -> {
                    // slack user not found in Space by email
                    slackPrincipal.slackUserProfile.realName ?: run {
                        log.warn("Slack user realName attribute is empty")
                        "<Unknown user>"
                    }
                }
                is SlackPrincipal.SlackBot -> {
                    slackPrincipal.botProfile.name ?: "<Unknown bot>"
                }
                is SlackPrincipal.Unknown -> {
                    slackPrincipal.id?.let {
                        "Unknown (id = $it)"
                    } ?: run {
                        "Unknown"
                    }
                }
            }

            PrincipalIn.Application(ApplicationIdentifier.Me) to spaceMessage(event, principalName)
        }
    }
}

suspend fun MessageFromSlackCtx.spaceMessage(
    event: MessageCreatedOrUpdated,
    principalName: String? = null
): ChatMessage {
    val mentionedSlackUserIds = getMentionedSlackUserIds(event.blocks)
    val slackUserById = mentionedSlackUserIds
        .mapNotNull { userId ->
            val slackProfile = slackClient.getUserById {
                it.user(userId)
            }
            if (slackProfile != null) {
                userId to slackProfile
            } else {
                null
            }
        }
        .toMap()

    val emails = slackUserById.values.mapNotNull { it.profile?.email }

    // TODO: add a method to Space API to get all records at once
    val spaceProfiles = emails.map {
        spaceClient.teamDirectory.profiles.getProfileByEmail(it) {
            id()
            username()
            emails {
                email()
            }
        }
    }

    val spaceProfileByEmail = spaceProfiles
        .flatMap { profile -> profile.emails.map { profileEmail -> profileEmail.email.lowercase() to profile } }
        .toMap()

    val slackUserDataById = slackUserById.map { (slackUserId, slackUserResponse) ->
        val email = slackUserResponse.profile.email
        val spaceProfile = spaceProfileByEmail[email]
        SlackUserData(slackUserId, slackUserResponse.userName(), spaceProfile)
    }.associateBy { it.id }

    val text = buildString {
        principalName?.let { append("**$it** says:\n") }
        buildMessageText(slackClient, slackTeam.domain, event.blocks, event.text, slackUserDataById)
    }
    return ChatMessage.Text(text)
}

suspend fun MessageFromSlackCtx.getSpaceUserId(
    slackPrincipal: SlackPrincipal,
): TD_MemberProfile? {
    val slackUserPrincipal = slackPrincipal as? SlackPrincipal.SlackUser ?: return null
    val slackUserEmail = slackUserPrincipal.slackUserProfile.email
        ?: return null // not sure — can a Slack user account be created without an email?

    return try {
        spaceClient.teamDirectory.profiles.getProfileByEmail(slackUserEmail) {
            id()
            username()
        }
    } catch (e: Exception) {
        null
    }
}

sealed class SlackPrincipal {
    abstract fun name(): String

    class SlackUser(val slackUserProfile: User.Profile) : SlackPrincipal() {
        override fun name(): String = slackUserProfile.realName
    }

    class SlackBot(val botProfile: BotProfile) : SlackPrincipal() {
        override fun name(): String = botProfile.name
    }

    class Unknown(val id: String?) : SlackPrincipal() {
        override fun name(): String = "<$id>"
    }
}

private suspend fun MessageFromSlackCtx.getSlackPrincipal(event: MessageCreatedOrUpdated): SlackPrincipal {
    val userId = event.userId
    val bot = event.botProfile
    return when {
        userId != null -> {
            val slackUserProfile = slackClient.getUserById {
                it.user(userId)
            }?.profile

            slackUserProfile?.let { SlackPrincipal.SlackUser(it) } ?: SlackPrincipal.Unknown(userId)
        }
        bot != null -> SlackPrincipal.SlackBot(bot)
        else -> SlackPrincipal.Unknown(null)
    }
}

private val log: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

private val imageFileExtensions = listOf("png", "gif", "jpg", "jpeg", "heic")

