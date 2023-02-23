package org.jetbrains.spaceSlackSync.slack

import com.slack.api.model.BotProfile
import com.slack.api.model.File
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
import space.jetbrains.api.runtime.types.partials.TD_MemberProfilePartial
import java.lang.invoke.MethodHandles

suspend fun MessageFromSlackCtx.getAttachments(event: MessageCreatedOrUpdated): List<AttachmentIn> {
    return event.files.orEmpty()
        .mapNotNull { getAttachment(it) }
}

private suspend fun MessageFromSlackCtx.getAttachment(it: File): AttachmentIn? {
    val url = it.urlPrivateDownload
    if (url == null) {
        log.error("Could not process attachment from Slack, File.urlPrivateDownload is null. Skipping the attachment.")
        return null
    }

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

    return when (it.filetype.lowercase()) {
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

suspend fun MessageFromSlackCtx.messageAuthorAndContent(event: MessageCreatedOrUpdated): Pair<PrincipalIn, ChatMessage> {
    val slackPrincipal = getSlackPrincipal(event)
    log.debug("Slack Principal: ${slackPrincipal.nameForLogging()}")

    val spaceUser = getSpaceTdMemberProfile(slackPrincipal, "message author")
    return if (spaceUser != null) {
        log.debug("Space user found, profile id: ${spaceUser.id}")
        PrincipalIn.Profile(ProfileIdentifier.Id(spaceUser.id)) to spaceMessage(event)
    } else {
        log.debug("Space user not found")
        PrincipalIn.Application(ApplicationIdentifier.Me) to spaceMessage(
            event,
            principalName = getPrincipalName(slackPrincipal)
        )
    }
}

private fun MessageFromSlackCtx.getPrincipalName(slackPrincipal: SlackPrincipal): String {
    val principalName = when (slackPrincipal) {
        is SlackPrincipal.SlackUser -> {
            // slack user not found in Space by email
            slackPrincipal.slackUserProfile.realName?.takeIf { it.isNotEmpty() }
                ?: slackPrincipal.slackUserProfile.displayName?.takeIf { it.isNotEmpty() }
                ?: slackPrincipal.slackUserProfile.email?.takeIf { it.isNotEmpty() }
                ?: run {
                    log.warn("Slack user realName, displayName and email attributes are all empty")
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
    return principalName
}

suspend fun MessageFromSlackCtx.spaceMessage(
    event: MessageCreatedOrUpdated,
    principalName: String? = null
): ChatMessage {
    val mentionedSlackUserIds = getMentionedSlackUserIds(event.blocks)
    val slackProfileResponseById = mentionedSlackUserIds
        .mapNotNull { userId ->
            val slackProfileResponse = slackClient.getUserById {
                it.user(userId)
            }
            if (slackProfileResponse != null) {
                log.debug("Mentioned user with Slack id $userId. Found Slack profile.")
                userId to slackProfileResponse
            } else {
                log.debug("Mentioned user with Slack id $userId. getUserById did not return the profile")
                null
            }
        }
        .toMap()

    val mentionedSlackPrincipalById = slackProfileResponseById
        .mapValues { (userId, slackProfileResponse) ->
            val profile = slackProfileResponse.profile
            if (profile == null) {
                SlackPrincipal.Unknown(userId)
            } else {
                SlackPrincipal.SlackUser(profile)
            }
        }

    // TODO: add a method to Space API to get all records at once
    val spaceProfiles = mentionedSlackPrincipalById.mapNotNull { (userId, slackPrincipal) ->
        getSpaceTdMemberProfile(slackPrincipal, "mentioned user ($userId)") {
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

    val slackUserDataById = slackProfileResponseById.map { (slackUserId, slackUserResponse) ->
        val email = slackUserResponse.profile.email.lowercase()
        val spaceProfile = spaceProfileByEmail[email]
        SlackUserData(slackUserId, slackUserResponse.userName(), spaceProfile)
    }.associateBy { it.id }

    return buildMessage(
        slackClient,
        slackTeam.domain,
        event.blocks,
        event.fields,
        event.text,
        slackUserDataById,
        event.color,
        principalName
    )
}

suspend fun MessageFromSlackCtx.getSpaceTdMemberProfile(
    slackPrincipal: SlackPrincipal,
    userTypeForLogging: String,
    fieldsToRetrieve: TD_MemberProfilePartial.() -> Unit = {
        id()
        username()
    },
): TD_MemberProfile? {
    val slackUserPrincipal = slackPrincipal as? SlackPrincipal.SlackUser
    if (slackUserPrincipal == null) {
        log.debug("Matching $userTypeForLogging from Slack to a user in Space: cannot identify user in Slack, user in Space will not be identified as well")
        return null
    }

    val slackUserEmail = slackUserPrincipal.slackUserProfile.email
    if (slackUserEmail == null) {
        log.debug("Matching $userTypeForLogging from Slack to a user in Space: Slack user does not have email, cannot identify Space user")
        return null
    }

    return try {
        spaceClient.teamDirectory.profiles.getProfileByEmail(slackUserEmail, verified = false) {
            fieldsToRetrieve()
        }
    } catch (e: Exception) {
        log.debug("Matching $userTypeForLogging from Slack to a user in Space: Space user not found by email from Slack")
        null
    }
}

sealed class SlackPrincipal {
    abstract fun nameForLogging(): String

    class SlackUser(val slackUserProfile: User.Profile) : SlackPrincipal() {
        override fun nameForLogging(): String = "<Slack User Profile>"
    }

    class SlackBot(val botProfile: BotProfile) : SlackPrincipal() {
        override fun nameForLogging(): String =
            botProfile.id?.takeIf { it.isNotEmpty() }?.let { "<Slack Bot Profile with id = $it>" }
                ?: "<Slack Bot Profile with no name or id>"
    }

    class Unknown(val id: String?) : SlackPrincipal() {
        override fun nameForLogging(): String = "<Unknown Slack Principal with id \"$id\">"
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

