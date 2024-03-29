package org.jetbrains.spaceSlackSync.space

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.jetbrains.spaceSlackSync.MDCParams
import org.jetbrains.spaceSlackSync.homepage.spaceHttpClient
import org.jetbrains.spaceSlackSync.newTraceId
import org.jetbrains.spaceSlackSync.platform.Server
import org.jetbrains.spaceSlackSync.withSpaceLogContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.Space
import space.jetbrains.api.runtime.SpaceClient
import space.jetbrains.api.runtime.helpers.*
import space.jetbrains.api.runtime.resources.applications
import space.jetbrains.api.runtime.types.*
import java.lang.invoke.MethodHandles

suspend fun processSpaceCall(call: ApplicationCall) {
    withContext(MDCContext(mapOf(MDCParams.SPACE_CALL_TRACE_ID to newTraceId()))) {
        log.debug("Incoming API call from Space")
        try {
            doProcessSpaceCall(call)
        } catch (e: Exception) {
            log.error("Exception while handling a call from Space", e)
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}

private suspend fun doProcessSpaceCall(call: ApplicationCall) {
    val ktorRequestAdapter = object : RequestAdapter {
        override suspend fun receiveText() =
            call.receiveText()

        override fun getHeader(headerName: String) =
            call.request.header(headerName)

        override suspend fun respond(httpStatusCode: Int, body: String) =
            call.respond(HttpStatusCode.fromValue(httpStatusCode), body)
    }

    Space.processPayload(
        ktorRequestAdapter, spaceHttpClient, spaceAppInstanceStorage,
        onAuthFailed = { message, _ ->
            log.warn("Space request authentication failed - $message")
            SpaceHttpResponse.RespondWithCode(HttpStatusCode.BadRequest)
        },
        payloadProcessor = { payload ->
            withSpaceLogContext(spaceAppClientId = appInstance.clientId, spaceUserId = payload.userId) {
                processSpacePayload(payload, call)
            }
        }
    )
}

private suspend fun ProcessingScope.processSpacePayload(
    payload: ApplicationPayload,
    call: ApplicationCall,
): SpaceHttpResponse {
    return when (payload) {
        is InitPayload -> handleAppInstallation(clientWithClientCredentials())

        is WebhookRequestPayload -> {
            when (val event = payload.payload) {
                is ChatMessageCreatedEvent,
                is ChatMessageDeletedEvent,
                is ChatMessageUpdatedEvent -> {
                    call.application.launch(Server + MDCContext()) {
                        processChatMessageFromSpace(event)
                    }
                }
            }
            SpaceHttpResponse.RespondWithOk
        }

        else -> {
            log.info("Processed payload type ${payload::class.simpleName}")
            SpaceHttpResponse.RespondWithOk
        }
    }
}

private suspend fun handleAppInstallation(spaceClient: SpaceClient): SpaceHttpResponse {
    try {
        doHandleAppInstallation(spaceClient)
        log.info("Processed InitPayload from Space")
    } catch (e: Exception) {
        log.error("Exception during processing InitPayload", e)
        return SpaceHttpResponse.RespondWithCode(HttpStatusCode.InternalServerError)
    }

    return SpaceHttpResponse.RespondWithOk
}

private suspend fun doHandleAppInstallation(spaceClient: SpaceClient) {
    spaceClient.applications.setUiExtensions(
        GlobalPermissionContextIdentifier,
        listOf(ApplicationHomepageUiExtensionIn("/space-iframe"))
    )

    spaceClient.applications.authorizations.authorizedRights.requestRights(
        application = ApplicationIdentifier.Me,
        contextIdentifier = GlobalPermissionContextIdentifier,
        rightCodes = listOf(
            PermissionIdentifier.ViewMemberProfiles
        )
    )
}

private val log: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
