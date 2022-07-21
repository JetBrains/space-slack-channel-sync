package org.jetbrains.spaceSlackSync.space

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.jetbrains.spaceSlackSync.MDCParams
import org.jetbrains.spaceSlackSync.homepage.spaceHttpClient
import org.jetbrains.spaceSlackSync.platform.Server
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.Space
import space.jetbrains.api.runtime.SpaceClient
import space.jetbrains.api.runtime.helpers.ProcessingScope
import space.jetbrains.api.runtime.helpers.RequestAdapter
import space.jetbrains.api.runtime.helpers.SpaceHttpResponse
import space.jetbrains.api.runtime.helpers.processPayload
import space.jetbrains.api.runtime.resources.applications
import space.jetbrains.api.runtime.types.*

suspend fun processSpaceCall(call: ApplicationCall) {
    try {
        doProcessSpaceCall(call)
    } catch (e: Exception) {
        log.error("Exception while handling a call from Space", e)
        call.respond(HttpStatusCode.InternalServerError)
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
        onAuthFailed = {
            log.warn("Space request authentication failed - $it")
            SpaceHttpResponse.RespondWithCode(HttpStatusCode.BadRequest)
        },
        payloadProcessor = { payload ->
            withContext(MDCContext(mapOf(MDCParams.SPACE_CLIENT_ID to appInstance.clientId))) {
                processSpacePayload(payload, this)
            }
        }
    )
}

private suspend fun ProcessingScope.processSpacePayload(payload: ApplicationPayload, scope: CoroutineScope): SpaceHttpResponse {
    return when (payload) {
        is InitPayload -> handleAppInstallation(clientWithClientCredentials())

        is WebhookRequestPayload -> {
            when (val event = payload.payload) {
                is ChatMessageCreatedEvent,
                is ChatMessageDeletedEvent,
                is ChatMessageUpdatedEvent -> {
                    scope.launch(Server) {
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
            "Profile.View",
        )
    )
}

private val log: Logger = LoggerFactory.getLogger("SpacePayloadProcessing")
