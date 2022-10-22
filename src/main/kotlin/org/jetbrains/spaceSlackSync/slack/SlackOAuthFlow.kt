package org.jetbrains.spaceSlackSync.slack

import com.slack.api.methods.response.oauth.OAuthV2AccessResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.jetbrains.spaceSlackSync.MDCParams
import org.jetbrains.spaceSlackSync.SlackCredentials
import org.jetbrains.spaceSlackSync.db
import org.jetbrains.spaceSlackSync.encrypted
import org.jetbrains.spaceSlackSync.routing.Routes
import org.jetbrains.spaceSlackSync.homepage.spaceHttpClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.SpaceAppInstance
import space.jetbrains.api.runtime.SpaceAuth
import space.jetbrains.api.runtime.SpaceClient
import space.jetbrains.api.runtime.resources.applications
import space.jetbrains.api.runtime.types.ApplicationIdentifier
import java.lang.invoke.MethodHandles
import java.time.LocalDateTime

suspend fun onAppInstalledToSlackTeam(call: ApplicationCall, spaceOrgId: String, params: Routes.SlackOAuthCallback) {
    if (params.code.isNullOrBlank()) {
        log.info("Authentication in Slack returned no auth code")
        val spaceAppInstance = getSpaceAppInstance(spaceOrgId, call) ?: return
        redirectBackToHomepageInSpace(spaceAppInstance, call)
        return
    }

    val response = requestOAuthToken(params.code)
    val accessToken = response?.accessToken
    val refreshToken = response?.refreshToken
    val accessTokenExpiresAt = LocalDateTime.now().plusSeconds(response?.expiresIn?.toLong() ?: 43200L)
    val teamId = response?.team?.id

    withContext(MDCContext(mapOf(MDCParams.SLACK_TEAM to teamId.orEmpty(), MDCParams.SPACE_CLIENT_ID to spaceOrgId))) {
        if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank() || teamId.isNullOrBlank()) {
            val message = "Could not fetch OAuth token from Slack. " +
                    "Team id = $teamId, " +
                    "access token is ${if (accessToken.isNullOrBlank()) "empty" else "provided"}, " +
                    "refresh token is ${if (refreshToken.isNullOrBlank()) "empty" else "provided"}, " +
                    "code = ${params.code}"
            call.respondError(HttpStatusCode.Unauthorized, log, message)
            return@withContext
        }

        val teamResponse = slackApiClient.methods(accessToken).teamInfo { it.team(teamId) }
        if (teamResponse.team == null) {
            call.respondError(
                HttpStatusCode.Unauthorized, log, "Could not fetch team info from Slack - ${teamResponse.error}"
            )
            return@withContext
        }

        val spaceAppInstance = getSpaceAppInstance(spaceOrgId, call) ?: return@withContext
        db.slackTeams.createOrUpdate(
            teamId,
            teamResponse.team.domain,
            spaceOrgId,
            accessToken.encrypted(),
            refreshToken.encrypted(),
            accessTokenExpiresAt
        )

        log.info("Slack team connected to Space org")
        redirectBackToHomepageInSpace(spaceAppInstance, call)
    }
}

private suspend fun redirectBackToHomepageInSpace(
    spaceAppInstance: SpaceAppInstance,
    call: ApplicationCall
) {
    val spaceClient = SpaceClient(spaceHttpClient, spaceAppInstance, SpaceAuth.ClientCredentials())
    val spaceApp = spaceClient.applications.getApplication(ApplicationIdentifier.Me)

    val backUrl = URLBuilder(spaceAppInstance.spaceServer.serverUrl).run {
        path("extensions", "installedApplications", "${spaceApp.name}-${spaceApp.id}", "homepage")
        buildString()
    }
    call.respondRedirect(backUrl)
}

private suspend fun getSpaceAppInstance(spaceOrgId: String, call: ApplicationCall) =
    db.spaceAppInstances.getById(spaceOrgId) ?: run {
        call.respondError(
            HttpStatusCode.BadRequest,
            log,
            "Unexpected value of the state query string parameter (flow id = $spaceOrgId)"
        )
        null
    }

fun requestOAuthToken(code: String): OAuthV2AccessResponse? {
    val response = slackApiClient.methods().oauthV2Access {
        it.clientId(SlackCredentials.clientId).clientSecret(SlackCredentials.clientSecret).code(code)
    }

    if (!response.isOk) {
        log.warn("Got ok=false while trying to refresh access token - ${response.error}")
        return null
    }

    return response
}

private val log: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
