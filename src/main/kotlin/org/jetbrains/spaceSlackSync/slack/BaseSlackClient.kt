package org.jetbrains.spaceSlackSync.slack

import com.slack.api.Slack
import com.slack.api.methods.SlackApiException
import com.slack.api.methods.SlackApiTextResponse
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.*
import org.apache.http.impl.nio.reactor.IOReactorConfig
import org.jetbrains.spaceSlackSync.SlackCredentials
import org.slf4j.Logger
import java.time.LocalDateTime

abstract class BaseSlackClient(
    accessToken: String,
    refreshToken: String,
    permissionScopes: String?,
    accessTokenExpiresAt: LocalDateTime,
    protected val teamId: String,
    protected val log: Logger
) {
    private var tokens: Tokens? = Tokens(accessToken, refreshToken, permissionScopes, accessTokenExpiresAt)

    protected suspend fun getRefreshedToken(): String? {
        val tokens = this.tokens ?: return null
        if (tokens.accessTokenExpiresAt.isBefore(LocalDateTime.now().plusSeconds(10))) {
            tryRefreshToken()
        }
        return tokens.accessToken
    }

    protected suspend fun <T : SlackApiTextResponse> fetch(action: String, handler: suspend (String) -> T?): T? {
        return tokens?.let {
            try {
                handler(it.accessToken).let { response: T? ->
                    if (response != null && !response.isOk) {
                        handleSlackError(action, response.error, handler)
                    } else {
                        response
                    }
                }
            } catch (ex: SlackApiException) {
                log.error("Failure fetching data from Slack", ex)
                handleSlackError(action, ex.error.error, handler)
            }
        }
    }

    private suspend fun <T : SlackApiTextResponse> handleSlackError(
        action: String,
        error: String,
        handler: (suspend (String) -> T?)?
    ): T? {
        if ((error == "token_expired" || error == "cannot_auth_user" || error == "invalid_auth") && handler != null) {
            tryRefreshToken()
            // break recursion on the second call to `handleSlackError` by omitting handler parameter
            return tokens?.let {
                try {
                    handler(it.accessToken).let { response: T? ->
                        if (response != null && !response.isOk) {
                            handleSlackError(action, response.error, handler = null)
                        } else {
                            response
                        }
                    }
                } catch (ex: SlackApiException) {
                    log.error("Failure fetching data from Slack", ex)
                    handleSlackError(action, ex.error.error, handler = null)
                }
            }
        }

        val shouldResetToken = slackErrorsToResetToken.contains(error)
        val slackUserTokenResetMessage = if (shouldResetToken) "Slack user refresh token is reset." else ""

        if (action to error !in noLogActionToResponse) {
            log.warn("Got ok=false from Slack on $action - $error. $slackUserTokenResetMessage")
        }

        if (shouldResetToken) {
            markTokenAsInvalid()
        }
        return null
    }

    private suspend fun tryRefreshToken() {
        log.info("Refreshing token...")
        val refreshToken = tokens?.refreshToken ?: return
        val permissionScopes = tokens?.permissionScopes
        tokens = null

        val response = slackApiClient.methods().oauthV2Access {
            it
                .clientId(SlackCredentials.clientId)
                .clientSecret(SlackCredentials.clientSecret)
                .grantType("refresh_token")
                .refreshToken(refreshToken)
        }
        if (!response.isOk) {
            log.warn("Got ok=false while trying to refresh access token - ${response.error}")
            if (response.error == "invalid_refresh_token") {
                val tokensFromDb = reloadTokensFromDb() ?: return
                if (tokensFromDb.refreshToken != refreshToken) {
                    tokens = tokensFromDb
                } else {
                    onInvalidRefreshToken()
                }
            }
            if (response.error == "invalid_client_id" || response.error == "bad_client_secret") {
                onInvalidAppCredentials()
            }
            return
        }

        val newAccessToken = response.accessToken ?: response.authedUser?.accessToken
        if (newAccessToken == null) {
            log.warn("Got ok response from Slack but no access token provided")
            return
        }

        val newRefreshToken =
            (response.refreshToken ?: response.authedUser?.refreshToken).takeUnless { it == refreshToken }
        log.info("Access token refreshed, ${if (newRefreshToken != null) "with" else "without"} new refresh token. Permission scope = ${response.authedUser?.scope}")
        tokens = Tokens(
            newAccessToken,
            refreshToken = newRefreshToken ?: refreshToken,
            permissionScopes = response.authedUser?.scope ?: permissionScopes,
            accessTokenExpiresAt = LocalDateTime.now().plusSeconds(response.expiresIn.toLong())
        ).also {
            updateTokensInDb(it)
        }
    }

    protected abstract suspend fun reloadTokensFromDb(): Tokens?

    protected abstract suspend fun updateTokensInDb(tokens: Tokens)

    protected abstract suspend fun onInvalidRefreshToken()

    protected abstract suspend fun onInvalidAppCredentials()

    protected abstract suspend fun markTokenAsInvalid()


    protected data class Tokens(
        val accessToken: String,
        val refreshToken: String,
        val permissionScopes: String? = null,
        val accessTokenExpiresAt: LocalDateTime
    )
}

val slackApiClient: Slack = Slack.getInstance()

/** Slack error codes that cause Slack refresh token reset and repeated request for the user to authenticate in Slack */
private val slackErrorsToResetToken = listOf(
    "invalid_auth",
    "account_inactive",
    "no_permission",
    "missing_scope",
    "not_allowed_token_type",
    "cannot_find_service"
)

private val noLogActionToResponse = listOf(
    SLACK_ACTION_LOOKUP_USER_BY_EMAIL to "users_not_found"
)

val httpClientForDownloads = HttpClient(Apache) {
    install(HttpTimeout) {
        this.socketTimeoutMillis = 30000
        this.connectTimeoutMillis = 30000
        this.requestTimeoutMillis = 30000
    }

    engine {
        customizeClient {
            setDefaultIOReactorConfig(
                IOReactorConfig.custom()
                    .setIoThreadCount(4)
                    .build()
            )
        }
    }
}
