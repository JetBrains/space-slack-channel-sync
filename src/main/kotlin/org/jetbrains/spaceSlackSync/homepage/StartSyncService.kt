package org.jetbrains.spaceSlackSync.homepage

import kotlinx.serialization.Serializable
import org.jetbrains.spaceSlackSync.db
import org.jetbrains.spaceSlackSync.routing.SpaceTokenInfo
import space.jetbrains.api.runtime.resources.applications
import space.jetbrains.api.runtime.types.*

class StartSyncService(spaceTokenInfo: SpaceTokenInfo) : ServiceBase(spaceTokenInfo) {
    suspend fun startSync(spaceChannelId: String, slackTeamId: String, slackChannelId: String): StartSyncResponse {
        db.syncedChannels.addIfAbsent(
            spaceTokenInfo.spaceAppInstance.clientId,
            spaceChannelId,
            slackTeamId,
            slackChannelId
        )

        appSpaceClient.applications.authorizations.authorizedRights.requestRights(
            application = ApplicationIdentifier.Me,
            contextIdentifier = ChannelPermissionContextIdentifier(spaceChannelId),
            rightCodes = listOf("Channel.ViewChannel", "Channel.ViewMessages", "Channel.ImportMessages")
        )

        val webhooks = appSpaceClient.applications.webhooks.getAllWebhooks(
            application = ApplicationIdentifier.Me,
        ) {
            webhook {
                id()
                subscriptions {
                    subscription {
                        filters()
                    }
                }
            }
        }

        webhooks.data
            .map { it.webhook }
            .forEach { webhook ->
                val isForThisChannel = webhook.subscriptions
                    .map { it.subscription.filters }
                    .any { filters ->
                        filters.any { filter ->
                            filter is ChatChannelSubscriptionFilter && filter.channel == spaceChannelId
                        }
                    }

                if (isForThisChannel) {
                    // webhook will be re-created
                    appSpaceClient.applications.webhooks.deleteWebhook(
                        application = ApplicationIdentifier.Me,
                        webhookId = webhook.id
                    )
                }
            }

        val webhookId = appSpaceClient.applications.webhooks.createWebhook(
            application = ApplicationIdentifier.Me,
            name = "Channel Messages",
            acceptedHttpResponseCodes = emptyList(),
        ).id

        appSpaceClient.applications.webhooks.subscriptions.createSubscription(
            application = ApplicationIdentifier.Me,
            webhookId = webhookId,
            name = "Channel Messages",
            subscription = CustomGenericSubscriptionIn(
                subjectCode = "Chat.Message",
                filters = listOf(
                    ChatChannelSubscriptionFilterIn(spaceChannelId)
                ),
                eventTypeCodes = listOf(
                    "Chat.Message.Created",
                    "Chat.Message.Deleted",
                    "Chat.Message.Updated",
                )
            ),
        )

        return StartSyncResponse(true)
    }
}

@Serializable
data class StartSyncResponse(
    val result: Boolean
)
