package org.jetbrains.spaceSlackSync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.jetbrains.spaceSlackSync.MDCParams.SLACK_CHANNEL_ID
import org.jetbrains.spaceSlackSync.MDCParams.SLACK_MESSAGE_ID
import org.jetbrains.spaceSlackSync.MDCParams.SLACK_TEAM_ID
import org.jetbrains.spaceSlackSync.MDCParams.SLACK_THREAD_ID
import org.jetbrains.spaceSlackSync.MDCParams.SLACK_USER_ID
import org.jetbrains.spaceSlackSync.MDCParams.SPACE_CHANNEL_ID
import org.jetbrains.spaceSlackSync.MDCParams.SPACE_CLIENT_ID
import org.jetbrains.spaceSlackSync.MDCParams.SPACE_MESSAGE_ID
import org.jetbrains.spaceSlackSync.MDCParams.SPACE_THREAD_ID
import org.jetbrains.spaceSlackSync.MDCParams.SPACE_USER_ID
import org.jetbrains.spaceSlackSync.slack.SlackMessageEvent
import org.jetbrains.spaceSlackSync.space.SpaceChatEventData
import org.jetbrains.spaceSlackSync.storage.SyncedChannel
import java.util.*

object MDCParams {
    const val SPACE_CLIENT_ID = "space_client_id"
    const val SPACE_CALL_TRACE_ID = "space_call_trace_id"
    const val SPACE_CHANNEL_ID = "space_channel_id"
    const val SPACE_MESSAGE_ID = "space_message_id"
    const val SPACE_THREAD_ID = "space_thread_id"
    const val SPACE_USER_ID = "space_user_id"

    const val SLACK_CALL_TRACE_ID = "slack_call_trace_id"
    const val SLACK_TEAM_ID = "slack_team_id"
    const val SLACK_CHANNEL_ID = "slack_channel_id"
    const val SLACK_MESSAGE_ID = "slack_message_id"
    const val SLACK_THREAD_ID = "slack_thread_id"
    const val SLACK_USER_ID = "slack_user_id"
}

fun newTraceId(): String = UUID.randomUUID().toString()

suspend inline fun <T> withSpaceLogContext(
    spaceAppClientId: String,
    spaceUserId: String?,
    noinline block: suspend CoroutineScope.() -> T
) = withLogContext(
    block,
    SPACE_CLIENT_ID to spaceAppClientId,
    SPACE_USER_ID to spaceUserId,
)

suspend inline fun <T> withSpaceMessageLogContext(
    data: SpaceChatEventData,
    noinline block: suspend CoroutineScope.() -> T
) = withLogContext(
    block,
    SPACE_CHANNEL_ID to data.spaceChannelId,
    SPACE_MESSAGE_ID to data.spaceMessageId,
    SPACE_THREAD_ID to data.spaceThreadId,
)

suspend inline fun <T> withSyncedChannelLogContext(
    syncedChannel: SyncedChannel,
    noinline block: suspend CoroutineScope.() -> T
) = withLogContext(
    block,
    SLACK_TEAM_ID to syncedChannel.slackTeamId,
    SLACK_CHANNEL_ID to syncedChannel.slackChannelId,
    SPACE_CLIENT_ID to syncedChannel.spaceAppClientId,
    SPACE_CHANNEL_ID to syncedChannel.spaceChannelId,
)

suspend inline fun <T> withSlackTeamLogContext(
    slackTeamId: String,
    noinline block: suspend CoroutineScope.() -> T
) = withLogContext(
    block,
    SLACK_TEAM_ID to slackTeamId,
)

suspend inline fun <T> withSlackEventLogContext(
    slackMessageEvent: SlackMessageEvent,
    noinline block: suspend CoroutineScope.() -> T
) = withLogContext(
    block,
    SLACK_TEAM_ID to slackMessageEvent.teamId,
    SLACK_CHANNEL_ID to slackMessageEvent.channelId,
    SLACK_MESSAGE_ID to slackMessageEvent.messageId,
    SLACK_THREAD_ID to slackMessageEvent.threadId,
    SLACK_USER_ID to slackMessageEvent.userId,
)

suspend inline fun <T> withLogContext(
    noinline block: suspend CoroutineScope.() -> T,
    vararg params: Pair<String, String?>,
): T {
    val map = (MDCContext().contextMap ?: emptyMap()).toMutableMap()
    params.forEach {
        it.second?.let { paramValue ->
            map[it.first] = paramValue
        }
    }
    return withContext(MDCContext(map), block)
}
