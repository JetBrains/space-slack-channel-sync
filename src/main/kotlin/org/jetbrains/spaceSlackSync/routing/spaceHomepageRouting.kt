package org.jetbrains.spaceSlackSync.routing

import com.nimbusds.jwt.JWTParser
import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.locations.*
import io.ktor.server.locations.post
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.jetbrains.spaceSlackSync.MDCParams
import org.jetbrains.spaceSlackSync.SlackCredentials
import org.jetbrains.spaceSlackSync.db
import org.jetbrains.spaceSlackSync.homepage.*
import org.jetbrains.spaceSlackSync.slack.slackAppClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.SpaceAppInstance

fun Routing.spaceHomepageRouting() {
    staticContent()

    get<Routes.ListSyncedChannels> {
        runAuthorized {
            val response = SyncedChannelsService(it).listSyncedChannels()
            call.respond(HttpStatusCode.OK, response)
        }
    }

    get<Routes.GetSlackWorkspaces> {
        runAuthorized { spaceTokenInfo ->
            val teams = db.slackTeams.getForSpaceOrg(spaceTokenInfo.spaceAppInstance.clientId).map { team ->
                val teamName = slackAppClient(team).let { slackClient ->
                    slackClient.getTeamInfo {
                        it.team(team.id)
                    }?.team?.name
                } ?: team.domain

                SlackWorkspaceOut(team.id, team.domain, teamName)
            }
            call.respond(HttpStatusCode.OK, SlackWorkspacesResponse(teams, true))
        }
    }

    get<Routes.UrlForAddingSlackTeam> {
        runAuthorized { spaceTokenInfo ->
            log.info("Adding Slack team to Space org")

            val installToSlackUrl = URLBuilder("https://slack.com/oauth/v2/authorize").run {
                parameters.apply {
                    append("client_id", SlackCredentials.clientId)
                    append("scope", slackAppPermissionScopes.joinToString(","))
                    append("user_scope", "")
                    append("state", "org-${spaceTokenInfo.spaceAppInstance.clientId}")
                }
                buildString()
            }
            call.respond(HttpStatusCode.OK, installToSlackUrl)
        }
    }

    get<Routes.MissingAppPermissions> {
        runAuthorized { spaceTokenInfo ->
            val response = MissingAppPermissions(spaceTokenInfo).getMissingAppPermissions()
            call.respond(HttpStatusCode.OK, response)
        }
    }

    get<Routes.SpaceChannelsToPickForSync> { params ->
        runAuthorized { spaceTokenInfo ->
            val response = SpaceChannelsToPickForSync(spaceTokenInfo).getSpaceChannelsToPickFrom(params.query)
            call.respond(HttpStatusCode.OK, response)
        }
    }

    get<Routes.SlackChannelsToPickForSync> { params ->
        runAuthorized { spaceTokenInfo ->
            val response =
                SlackChannelsToPickForSync(spaceTokenInfo).getSlackChannelsToPickFrom(params.slackTeamId, params.query)
            call.respond(HttpStatusCode.OK, response)
        }
    }

    post<Routes.StartSync> { params ->
        runAuthorized { spaceTokenInfo ->
            val response = StartSyncService(spaceTokenInfo).startSync(
                params.spaceChannelId,
                params.slackTeamId,
                params.slackChannelId
            )
            call.respond(HttpStatusCode.OK, response)
        }
    }

    post<Routes.StopSync> { params ->
        runAuthorized { spaceTokenInfo ->
            val response = StopSyncService(spaceTokenInfo).stopSync(
                params.spaceChannelId,
                params.slackTeamId,
                params.slackChannelId
            )
            call.respond(HttpStatusCode.OK, response)
        }
    }
}

private fun Routing.staticContent() {
    // TODO: make index.html refer to js file inside `space-iframe` to simplify this routing
    static("/space-iframe") {
        staticBasePackage = "space-iframe"
        resources(".")
        defaultResource("index.html")
    }
    static("/") {
        staticBasePackage = "space-iframe"
        resources(".")
    }
}

data class SpaceTokenInfo(
    val spaceAppInstance: SpaceAppInstance,
    val spaceUserId: String,
    val spaceAccessToken: String,
)

private suspend fun PipelineContext<Unit, ApplicationCall>.runAuthorized(handler: suspend (SpaceTokenInfo) -> Unit) {
    getSpaceTokenInfo()?.let { spaceTokenInfo ->
        withContext(MDCContext(mapOf(MDCParams.SPACE_CLIENT_ID to spaceTokenInfo.spaceAppInstance.clientId))) {
            try {
                handler(spaceTokenInfo)
            } catch (e: Exception) {
                log.error("Exception while processing the \"${call.request.uri}\" call from homepage UI", e)
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    } ?: run {
        call.respond(HttpStatusCode.Unauthorized, "Invalid access token")
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.getSpaceTokenInfo(): SpaceTokenInfo? =
    (context.request.parseAuthorizationHeader() as? HttpAuthHeader.Single)?.blob
        ?.let { getSpaceTokenInfo(it) }

private suspend fun getSpaceTokenInfo(spaceUserToken: String): SpaceTokenInfo? {
    val jwtClaimsSet = JWTParser.parse(spaceUserToken).jwtClaimsSet
    val spaceAppInstance = jwtClaimsSet.audience.singleOrNull()?.let { db.spaceAppInstances.getById(it) } ?: return null
    val spaceUserId = jwtClaimsSet.subject
    return SpaceTokenInfo(spaceAppInstance, spaceUserId, spaceUserToken)
}

val slackAppPermissionScopes = listOf(
    "users.profile:read",
    "users:read",
    "users:read.email",
    "channels:read",
    "channels:history",
    "team:read",
    "chat:write",
    "chat:write.customize",
    "files:read",
    "remote_files:write",
)

@Serializable
data class SlackWorkspacesResponse(val workspaces: List<SlackWorkspaceOut>, val canManage: Boolean)

@Serializable
data class SlackWorkspaceOut(val id: String, val domain: String, val name: String)

private val log: Logger = LoggerFactory.getLogger("SpaceAppHomepage")
