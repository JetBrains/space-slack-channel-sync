package org.jetbrains.spaceSlackSync.slack

import com.slack.api.model.Conversation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

suspend fun retrieveAllSlackChannels(
    slackClient: SlackClient,
    slackTeamId: String,
): List<Conversation> {
    return retrieveSlackChannels(
        query = "",
        slackClient = slackClient,
        slackTeamId = slackTeamId,
        requestBatchSize = CHANNEL_BATCH_SIZE,
        responseBatchSize = null,
        conversationFilter = { true },
    )
}

private suspend fun retrieveSlackChannels(
    query: String,
    slackClient: SlackClient,
    slackTeamId: String,
    requestBatchSize: Int?,
    responseBatchSize: Int?,
    conversationFilter: (Conversation) -> Boolean,
): List<Conversation> {
    val results = mutableListOf<Conversation>()
    var cursor: String? = null

    var numberOfLoops = 0
    for (i in 1..MAX_REQUESTS) {
        numberOfLoops++
        val slackChannels = slackClient.getChannelsRaw {
            cursor?.let { currentCursor -> it.cursor(currentCursor) }
            requestBatchSize?.let { requestBatchSize -> it.limit(requestBatchSize) }
            it.teamId(slackTeamId)
        } ?: break

        val channelsToAdd = slackChannels.channels
            .filter { conversationFilter(it) }
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }

        results.addAll(channelsToAdd)

        if (responseBatchSize != null && results.size >= responseBatchSize) {
            break
        }

        cursor = slackChannels.responseMetadata.nextCursor
        if (cursor.isNullOrEmpty()) {
            break
        }
    }

    log.debug("It took $numberOfLoops getChannels calls")

    return results
}

private const val MAX_REQUESTS = 50
private const val CHANNEL_BATCH_SIZE = 1000

private val log: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
