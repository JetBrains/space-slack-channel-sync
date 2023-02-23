package org.jetbrains.spaceSlackSync.routing

import io.ktor.server.locations.*

object Routes {
    @Location("/api/synced-channels")
    object ListSyncedChannels

    @Location("/api/slack-workspaces")
    object GetSlackWorkspaces

    @Location("/api/url-for-adding-slack-team")
    object UrlForAddingSlackTeam

    @Location("/api/missing-app-permissions")
    object MissingAppPermissions

    @Location("/api/space-channels-to-pick-for-sync")
    class SpaceChannelsToPickForSync(val query: String)

    @Location("/api/slack-channels-to-pick-for-sync")
    class SlackChannelsToPickForSync(val slackTeamId: String, val query: String)

    @Location("/api/start-sync")
    class StartSync(val spaceChannelId: String, val slackTeamId: String, val slackChannelId: String)

    @Location("/api/stop-sync")
    class StopSync(val spaceChannelId: String, val slackTeamId: String, val slackChannelId: String)

    // TODO: implement
    @Location("/api/remove-slack-team")
    data class RemoveSlackTeam(val slackTeamId: String)

    /** Space-facing application endpoint for all types of payload */
    @Location("/api/space")
    object SpaceApiEndpoint

    /**
     * Callback for the end of OAuth flow in Slack
     * Is called by Slack when the application is installed to a new Slack team.
     * */
    @Location("/api/slack/oauth/callback")
    data class SlackOAuthCallback(val state: String? = null, val code: String? = null)

    /** Is called by Slack to notify application of the new events */
    @Location("/api/slack/events")
    object SlackEvents
}
