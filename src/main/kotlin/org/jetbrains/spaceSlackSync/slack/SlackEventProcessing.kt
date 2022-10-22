package org.jetbrains.spaceSlackSync.slack

import com.slack.api.app_backend.SlackSignature
import com.slack.api.app_backend.events.EventTypeExtractorImpl
import com.slack.api.app_backend.events.payload.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.spaceSlackSync.SlackCredentials
import org.jetbrains.spaceSlackSync.db
import org.jetbrains.spaceSlackSync.gson
import org.jetbrains.spaceSlackSync.homepage.SlackTeamCache
import org.jetbrains.spaceSlackSync.platform.Server
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles


private val slackEventTypeExtractor = EventTypeExtractorImpl()
private val slackSignatureVerifier =
    SlackCredentials.signingSecret?.let { SlackSignature.Verifier(SlackSignature.Generator(it)) }

suspend fun onSlackEvent(call: ApplicationCall) {
    val timestamp = call.request.header(SlackSignature.HeaderNames.X_SLACK_REQUEST_TIMESTAMP)
    val signature = call.request.header(SlackSignature.HeaderNames.X_SLACK_SIGNATURE)
    if (timestamp == null || signature == null) {
        call.respondError(
            HttpStatusCode.BadRequest,
            log,
            "HTTP headers ${SlackSignature.HeaderNames.X_SLACK_REQUEST_TIMESTAMP} and ${SlackSignature.HeaderNames.X_SLACK_SIGNATURE} are required"
        )
        return
    }
    val requestBody = call.receiveText()
    if (slackSignatureVerifier?.isValid(timestamp, requestBody, signature) == false) {
        call.respondError(HttpStatusCode.BadRequest, log, "Invalid request signature")
        return
    }

    val requestBodyJson = Json.parseToJsonElement(requestBody)
    when (val payloadType = requestBodyJson.jsonObject["type"]?.jsonPrimitive?.content) {
        "url_verification" -> {
            val challenge = requestBodyJson.jsonObject["challenge"]?.jsonPrimitive?.content
            challenge?.let { call.respondText(it) }
                ?: call.respondError(HttpStatusCode.BadRequest, log, "Challenge expected in url verification request")
            return
        }

        "event_callback" -> {
            call.application.launch(Server) {
                // process event asynchronously
                processEventFromSlack(requestBody, requestBodyJson)
            }
        }

        else -> log.warn("Unexpected Slack event payload type - $payloadType")
    }
    call.respond(HttpStatusCode.OK)
}

private suspend fun processEventFromSlack(requestBody: String, requestBodyJson: JsonElement) {
    try {
        doProcessEventFromSlack(requestBody, requestBodyJson)
    } catch (e: Exception) {
        log.error("Exception during processing event from Slack", e)
    }
}

private suspend fun doProcessEventFromSlack(requestBody: String, requestBodyJson: JsonElement) {
    when (val eventType = slackEventTypeExtractor.extractEventType(requestBody)) {
        "team_domain_change" -> {
            val evt = gson.fromJson(requestBody, TeamDomainChangePayload::class.java)
            db.slackTeams.updateDomain(evt.teamId, evt.event.domain)
        }

        "app_uninstalled" -> {
            val evt = gson.fromJson(requestBody, AppUninstalledPayload::class.java)
            db.slackTeams.markTokenAsInvalid(evt.teamId)
        }

        "message" -> {
            processMessageEvent(requestBodyJson, requestBody)
        }

        "channel_created" -> {
            val evt = gson.fromJson(requestBody, ChannelCreatedPayload::class.java)
            evt.teamId?.let { teamId -> SlackTeamCache.clearCacheForTeam(teamId) }
        }

        "channel_deleted" -> {
            val evt = gson.fromJson(requestBody, ChannelDeletedPayload::class.java)
            evt.teamId?.let { teamId -> SlackTeamCache.clearCacheForTeam(teamId) }
        }

        "channel_archive" -> {
            val evt = gson.fromJson(requestBody, ChannelArchivePayload::class.java)
            evt.teamId?.let { teamId -> SlackTeamCache.clearCacheForTeam(teamId) }
        }

        "channel_unarchive" -> {
            val evt = gson.fromJson(requestBody, ChannelUnarchivePayload::class.java)
            evt.teamId?.let { teamId -> SlackTeamCache.clearCacheForTeam(teamId) }
        }

        else ->
            log.warn("Unprocessed Slack event type - $eventType")
    }
}

private val log: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
