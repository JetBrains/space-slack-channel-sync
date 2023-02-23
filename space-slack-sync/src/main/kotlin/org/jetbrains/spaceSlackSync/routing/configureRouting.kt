package org.jetbrains.spaceSlackSync.routing

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.locations.*
import io.ktor.server.locations.post
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.jetbrains.spaceSlackSync.MDCParams
import org.jetbrains.spaceSlackSync.newTraceId
import org.jetbrains.spaceSlackSync.slack.onAppInstalledToSlackTeam
import org.jetbrains.spaceSlackSync.slack.onSlackEvent
import org.jetbrains.spaceSlackSync.space.processSpaceCall


fun Application.configureRouting() {
    install(Locations)
    install(ContentNegotiation) {
        json()
    }

    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK)
        }

        spaceHomepageRouting()

        post<Routes.SpaceApiEndpoint> {
            processSpaceCall(call)
        }

        get<Routes.SlackOAuthCallback> { params ->
            val flowId = params.state
            when {
                flowId.isNullOrBlank() ->
                    call.respond(HttpStatusCode.BadRequest, "Expected state param in callback request")

                flowId.startsWith("org-") ->
                    onAppInstalledToSlackTeam(call, flowId.removePrefix("org-"), params)

                else ->
                    call.respond(HttpStatusCode.BadRequest, "Malformed state param in callback request")
            }
        }

        post<Routes.SlackEvents> {
            withContext(MDCContext(mapOf(MDCParams.SLACK_CALL_TRACE_ID to newTraceId()))) {
                onSlackEvent(call)
            }
        }
    }
}
