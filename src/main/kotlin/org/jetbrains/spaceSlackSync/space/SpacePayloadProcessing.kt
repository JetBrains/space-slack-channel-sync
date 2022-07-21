package org.jetbrains.spaceSlackSync.space

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.jetbrains.spaceSlackSync.platform.launch
import org.jetbrains.spaceSlackSync.homepage.spaceHttpClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import space.jetbrains.api.runtime.Space
import space.jetbrains.api.runtime.SpaceClient
import space.jetbrains.api.runtime.helpers.RequestAdapter
import space.jetbrains.api.runtime.helpers.SpaceHttpResponse
import space.jetbrains.api.runtime.helpers.processPayload
import space.jetbrains.api.runtime.resources.applications
import space.jetbrains.api.runtime.types.*

suspend fun onSpaceCall(call: ApplicationCall) {
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
            when (payload) {
                is InitPayload -> {
                    onAppInstalledToSpaceOrg(clientWithClientCredentials())
                    SpaceHttpResponse.RespondWithOk
                }

                is WebhookRequestPayload -> {
                    when (val event = payload.payload) {
                        is ChatMessageCreatedEvent,
                        is ChatMessageDeletedEvent,
                        is ChatMessageUpdatedEvent -> {
                            launch {
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
    )
}

suspend fun onAppInstalledToSpaceOrg(spaceClient: SpaceClient) {
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
