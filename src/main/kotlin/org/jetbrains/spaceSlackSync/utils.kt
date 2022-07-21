package org.jetbrains.spaceSlackSync

import com.google.gson.Gson
import com.slack.api.util.json.GsonFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext

object MDCParams {
    const val SPACE_ORG = "space_org"
    const val SPACE_USER = "space_user"
    const val SLACK_TEAM = "slack_team"
    const val SLACK_USER = "slack_user"
}

suspend inline fun <T> withSpaceLogContext(
    spaceAppClientId: String,
    spaceUserId: String,
    slackTeamId: String,
    vararg params: Pair<String, String>,
    noinline block: suspend CoroutineScope.() -> T
) =
    MDCParams
        .run { mapOf(SPACE_ORG to spaceAppClientId, SPACE_USER to spaceUserId, SLACK_TEAM to slackTeamId) + params }
        .let { withContext(MDCContext(it), block) }

suspend inline fun <T> withSlackLogContext(
    slackTeamId: String,
    slackUserId: String,
    spaceAppClientId: String,
    vararg params: Pair<String, String>,
    noinline block: suspend CoroutineScope.() -> T
) =
    MDCParams
        .run { mapOf(SLACK_TEAM to slackTeamId, SLACK_USER to slackUserId, SPACE_ORG to spaceAppClientId) + params }
        .let { withContext(MDCContext(it), block) }

val gson: Gson = GsonFactory.createSnakeCase()
