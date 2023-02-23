import { fetchFromServer } from "./fetch";
import * as slackWorkspaces from "./slackTeams";
import { ChannelSelectOption } from "../components/select"

let defaultChannelsBatch: Array<SlackChannelToPickForSync> | null = null;

export interface SlackChannelToPickForSync {
    readonly channelNameInSlack: string;
    readonly slackChannelId: string;
}

export interface SlackChannelsToPickForSyncResponse {
    readonly slackChannelsToPickForSync: Array<SlackChannelToPickForSync>;
}

async function getSlackChannelsBatch(slackTeamId: string, query: string): Promise<SlackChannelToPickForSync[]> {
    const response: SlackChannelsToPickForSyncResponse = await fetchFromServer(`/api/slack-channels-to-pick-for-sync?slackTeamId=${slackTeamId}&query=${query}`);
    return response.slackChannelsToPickForSync;
}

export async function retrieveDefaultChannelBatch(slackTeamId: string, recalc: boolean) {
    if (defaultChannelsBatch == null || recalc) {
        defaultChannelsBatch = await getSlackChannelsBatch(slackTeamId, "");
    }
}

export function getDefaultChannelsAsSelectOptions(): ChannelSelectOption[] {
    if (defaultChannelsBatch == null) {
        return [];
    }

    return defaultChannelsBatch.map((channel) => channelToSelectOption(channel));
}

export const loadChannelsAsSelectOptions = (query: string, callback: (options: ChannelSelectOption[]) => void) => {
    const loadChannels = async () => {
        const slackWorkspace = slackWorkspaces.getSelectedSlackWorkspace();
        let channels = slackWorkspace == null ? [] : await getSlackChannelsBatch(slackWorkspace.id, query);
        callback(channels.map((channel) => channelToSelectOption(channel)));
    };
    loadChannels().catch(console.error);
};

function channelToSelectOption(channel: SlackChannelToPickForSync): ChannelSelectOption {
    return {
        value: channel.slackChannelId,
        label: channel.channelNameInSlack,
        optionType: "Slack",
        icon: ""
    }
}
