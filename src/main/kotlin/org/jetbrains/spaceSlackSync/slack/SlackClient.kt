package org.jetbrains.spaceSlackSync.slack

import com.slack.api.RequestConfigurator
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.methods.request.chat.ChatUpdateRequest
import com.slack.api.methods.request.conversations.ConversationsListRequest
import com.slack.api.methods.request.team.TeamInfoRequest
import com.slack.api.methods.request.usergroups.UsergroupsListRequest
import com.slack.api.methods.request.users.profile.UsersProfileGetRequest
import com.slack.api.methods.response.conversations.ConversationsListResponse
import com.slack.api.methods.response.users.profile.UsersProfileGetResponse
import com.slack.api.model.Conversation
import com.slack.api.model.User
import io.ktor.client.call.*
import io.ktor.client.request.*
import org.jetbrains.spaceSlackSync.db
import org.jetbrains.spaceSlackSync.decrypted
import org.jetbrains.spaceSlackSync.encrypted
import org.jetbrains.spaceSlackSync.storage.SlackTeam
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import kotlin.time.measureTime

const val SLACK_ACTION_LOOKUP_USER_BY_EMAIL = "lookup user by email"

/** Slack client for talking to Slack on behalf of the application itself.
 *  Automatically refreshes token when it expires.
 **/
class SlackClient constructor(
    teamId: String,
    accessToken: String,
    refreshToken: String,
    accessTokenExpiresAt: LocalDateTime
) : BaseSlackClient(accessToken, refreshToken, permissionScopes = null, accessTokenExpiresAt, teamId, log) {

    companion object {
        suspend fun tryCreate(teamId: String): SlackClient? =
            db.slackTeams.getById(teamId)?.let { team ->
                slackAppClient(team)
            }
    }

    // TODO: do this as a flow of chunks instead
    suspend fun downloadFile(url: String): ByteArray {
        return httpClientForDownloads.get(url) {
            header("Authorization", "Bearer ${getRefreshedToken()}")
        }.body()
    }

    suspend fun getChannelInfo(slackChannelId: String): Conversation? {
        return fetch("get channel info from Slack") { accessToken ->
            slackApiClient.methods(accessToken).conversationsInfo {
                it.channel(slackChannelId)
            }
        }?.channel
    }

    suspend fun getChannelsRaw(builder: RequestConfigurator<ConversationsListRequest.ConversationsListRequestBuilder>): ConversationsListResponse? {
        var response: ConversationsListResponse? = null
        val duration = measureTime {
            response = fetch("get channel list from Slack") { accessToken ->
                slackApiClient.methods(accessToken).conversationsList(builder)
            }
        }

        log.debug("getChannels took $duration")

        return response
    }

    suspend fun postMessage(builder: RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>) =
        fetch("post message to Slack channel") { accessToken ->
            slackApiClient.methods(accessToken).chatPostMessage(builder)
        }

    suspend fun editMessage(builder: RequestConfigurator<ChatUpdateRequest.ChatUpdateRequestBuilder>) =
        fetch("post message to Slack channel") { accessToken ->
            slackApiClient.methods(accessToken).chatUpdate(builder)
        }

    suspend fun deleteMessage(channelId: String, messageId: String) {
        fetch("delete message in Slack") { accessToken ->
            slackApiClient.methods(accessToken).chatDelete {
                it.channel(channelId)
                it.ts(messageId)
            }
        }
    }

    suspend fun getUserById(userId: String): User.Profile? {
        return fetch("lookup user by id in Slack") { accessToken ->
            slackApiClient.methods(accessToken).usersProfileGet {
                it.user(userId)
            }
        }?.profile
    }

    suspend fun getUserById(builder: RequestConfigurator<UsersProfileGetRequest.UsersProfileGetRequestBuilder>) =
        fetch("lookup user by id in Slack") { accessToken ->
            slackApiClient.methods(accessToken).usersProfileGet(builder)
        }

    suspend fun tryLookupUserByEmail(email: String): User? {
        return try {
            fetch(SLACK_ACTION_LOOKUP_USER_BY_EMAIL) { accessToken ->
                slackApiClient.methods(accessToken).usersLookupByEmail {
                    it.email(email)
                }
            }?.user
        } catch (e: Exception) {
            log.debug("Slack user not found by email. Slack team id = $teamId")
            null
        }
    }

    suspend fun getTeamInfo(builder: RequestConfigurator<TeamInfoRequest.TeamInfoRequestBuilder>) =
        fetch("get team info in Slack") { accessToken ->
            slackApiClient.methods(accessToken).teamInfo(builder)
        }

    suspend fun getUserGroups(builder: RequestConfigurator<UsergroupsListRequest.UsergroupsListRequestBuilder>) =
        fetch("get team info in Slack") { accessToken ->
            slackApiClient.methods(accessToken).usergroupsList(builder)
        }

    suspend fun addRemoteFile(externalId: String, filePublicUrl: String, title: String) {
        fetch("get team info in Slack") { accessToken ->
            slackApiClient.methods(accessToken).filesRemoteAdd {
                it.externalId(externalId)
                it.externalUrl(filePublicUrl)
                it.title(title)
            }
        }
    }

    override suspend fun reloadTokensFromDb(): Tokens? {
        val team = db.slackTeams.getById(teamId)
        if (team == null) {
            log.info("Refresh token cannot be retrieved from storage")
            return null
        }

        return Tokens(
            team.appAccessToken.decrypted(),
            team.appRefreshToken.decrypted(),
            accessTokenExpiresAt = team.accessTokenExpiresAt
        )
    }

    override suspend fun updateTokensInDb(tokens: Tokens) {
        db.slackTeams.updateTokens(
            teamId,
            tokens.accessToken.encrypted(),
            tokens.refreshToken.encrypted(),
            tokens.accessTokenExpiresAt
        )
    }

    override suspend fun markTokenAsInvalid() {
        db.slackTeams.markTokenAsInvalid(teamId)
    }

    override suspend fun onInvalidRefreshToken() {
        markTokenAsInvalid()
    }

    override suspend fun onInvalidAppCredentials() {
        markTokenAsInvalid()
    }
}

private val log: Logger = LoggerFactory.getLogger(SlackClient::class.java)

fun slackAppClient(team: SlackTeam) =
    SlackClient(team.id, team.appAccessToken.decrypted(), team.appRefreshToken.decrypted(), team.accessTokenExpiresAt)

suspend fun Conversation.channelLink(slackClient: SlackClient, slackDomain: String, id: String) =
    if (isIm)
        slackClient.getUserById {
            it.user(user)
        }?.userName()
            ?.let { "[DM with $it](https://$slackDomain.slack.com/archives/$id)" }
    else
        name?.let { "[#$it](https://$slackDomain.slack.com/archives/$id)" }

fun UsersProfileGetResponse.userName() =
    profile?.let { userProfile ->
        userProfile.realName?.takeUnless { it.isBlank() } ?: userProfile.displayName?.takeUnless { it.isBlank() }
    }
