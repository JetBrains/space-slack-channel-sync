package org.jetbrains.spaceSlackSync.slack

import com.slack.api.app_backend.SlackSignature
import com.slack.api.app_backend.events.EventTypeExtractorImpl
import com.slack.api.app_backend.events.payload.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.spaceSlackSync.SlackCredentials
import org.jetbrains.spaceSlackSync.db
import org.jetbrains.spaceSlackSync.gson
import org.jetbrains.spaceSlackSync.homepage.SlackTeamCache
import org.jetbrains.spaceSlackSync.platform.Server
import org.jetbrains.spaceSlackSync.withSlackTeamLogContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import kotlin.reflect.KClass


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
        log.warn("`timestamp` or `signature` headers are missing, responded with 400")
        return
    }

    val requestBody = call.receiveText()
    if (slackSignatureVerifier?.isValid(timestamp, requestBody, signature) == false) {
        call.respondError(HttpStatusCode.BadRequest, log, "Invalid request signature")
        log.warn("Invalid Slack request signature, responded with 400")
        return
    }

    val requestBodyJson = Json.parseToJsonElement(requestBody)
    when (val payloadType = requestBodyJson.jsonObject["type"]?.jsonPrimitive?.content) {
        "url_verification" -> {
            log.debug("Slack payload type: url_verification")
            val challenge = requestBodyJson.jsonObject["challenge"]?.jsonPrimitive?.content
            challenge?.let { call.respondText(it) }
                ?: call.respondError(HttpStatusCode.BadRequest, log, "Challenge expected in url verification request")
            return
        }

        "event_callback" -> {
            log.debug("Slack payload type: event_callback")
            call.application.launch(Server + MDCContext()) {
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
        val eventType = slackEventTypeExtractor.extractEventType(requestBody)
        log.debug("Slack event type: $eventType")
        doProcessEventFromSlack(requestBody, requestBodyJson, eventType)
    } catch (e: Exception) {
        log.error("Exception during processing event from Slack", e)
    }
}

private suspend fun doProcessEventFromSlack(requestBody: String, requestBodyJson: JsonElement, eventType: String) {
    when (eventType) {
        "team_domain_change" -> {
            val evt = gson.fromJson(requestBody, TeamDomainChangePayload::class.java)
            val teamId = evt.teamId
            withSlackTeamLogContext(teamId) {
                db.slackTeams.updateDomain(evt.teamId, evt.event.domain)
                log.debug("Slack domain updated")
            }
        }

        "app_uninstalled" -> {
            val evt = gson.fromJson(requestBody, AppUninstalledPayload::class.java)
            val teamId = evt.teamId
            withSlackTeamLogContext(teamId) {
                db.slackTeams.markTokenAsInvalid(teamId)
                log.debug("Slack application uninstalled, marked token as invalid")
            }
        }

        "message" -> {
            processMessageEvent(requestBodyJson, requestBody)
        }

        "channel_created" -> {
            processSlackEventForCacheClearing(requestBody, ChannelCreatedPayload::class)
        }

        "channel_deleted" -> {
            processSlackEventForCacheClearing(requestBody, ChannelDeletedPayload::class)
        }

        "channel_archive" -> {
            processSlackEventForCacheClearing(requestBody, ChannelArchivePayload::class)
        }

        "channel_unarchive" -> {
            processSlackEventForCacheClearing(requestBody, ChannelUnarchivePayload::class)
        }

        else ->
            log.warn("Unprocessed Slack event type - $eventType")
    }
}

private suspend fun <T: EventsApiPayload<*>> processSlackEventForCacheClearing(requestBody: String, payloadKClass: KClass<T>) {
    val evt = gson.fromJson(requestBody, payloadKClass.java)
    val teamId = evt.teamId
    if (teamId == null) {
        log.warn("Slack teamId is `null` in the ${payloadKClass.simpleName} event")
    } else {
        withSlackTeamLogContext(teamId) {
            teamId.let { teamId -> SlackTeamCache.clearCacheForTeam(teamId) }
            log.debug("Slack team cache cleared on ${payloadKClass.simpleName} event")
        }
    }
}

private val log: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
