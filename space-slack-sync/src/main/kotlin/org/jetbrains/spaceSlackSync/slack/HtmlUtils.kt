package org.jetbrains.spaceSlackSync.slack

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import kotlinx.html.*
import org.slf4j.Logger

suspend fun ApplicationCall.respondError(statusCode: HttpStatusCode, log: Logger, message: String) {
    log.warn(message)
    respondHtml(statusCode) {
        errorPage(message)
    }
}

private fun HTML.errorPage(message: String) = page {
    h3 { +"Something went wrong..." }

    div {
        classes = setOf("error")
        +message
    }
}

private fun HTML.page(initHead: HEAD.() -> Unit = {}, initBody: DIV.() -> Unit) {
    head {
        styleLink("/static/installPage.css")
        initHead()
    }
    body {
        div {
            classes = setOf("main")

            initBody()
        }
    }
}
