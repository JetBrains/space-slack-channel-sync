import fetchFromServer from "./fetch";
import * as slackWorkspaces from "./slackTeams.js";

let defaultChannelsBatch = null;

async function getSlackChannelsBatch(slackTeamId, query) {
    let response = await fetchFromServer(`/api/slack-channels-to-pick-for-sync?slackTeamId=${slackTeamId}&query=${query}`);
    return (await response.json()).slackChannelsToPickForSync;
}

export async function retrieveDefaultChannelBatch(slackTeamId, recalc) {
    if (defaultChannelsBatch == null || recalc) {
        defaultChannelsBatch = await getSlackChannelsBatch(slackTeamId, "");
    }
}

export function getDefaultChannelsAsSelectOptions() {
    return defaultChannelsBatch.map((channel) => channelToSelectOption(channel));
}

export const loadChannelsAsSelectOptions = (query, callback) => {
    const loadChannels = async () => {
        let channels = await getSlackChannelsBatch(slackWorkspaces.getSelectedSlackWorkspace().id, query);
        callback(channels.map((channel) => channelToSelectOption(channel)));
    };
    loadChannels().catch(console.error);
};

function channelToSelectOption(channel) {
    return {value: channel.slackChannelId, label: channel.channelNameInSlack, optionType: "Slack"}
}
