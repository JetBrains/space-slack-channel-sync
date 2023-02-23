import { ChannelSelectOption, OptionsCallback } from "../components/select";
import { fetchFromServer } from "./fetch";

let defaultChannelsBatch: SpaceChannelToPickForSync[] | null = null;

interface SpaceChannelsToPickForSyncResponse {
    spaceChannelsToPickForSync: SpaceChannelToPickForSync[];
}

interface SpaceChannelToPickForSync {
    channelNameInSpace: string;
    spaceChannelId: string;
    icon: string | null;
}

async function getSpaceChannelsBatch(query: string): Promise<SpaceChannelToPickForSync[]> {
    let response: SpaceChannelsToPickForSyncResponse = await fetchFromServer(`/api/space-channels-to-pick-for-sync?query=${query}`);
    return response.spaceChannelsToPickForSync;
}

export async function retrieveDefaultChannelBatch(recalc: boolean) {
    if (defaultChannelsBatch == null || recalc) {
        defaultChannelsBatch = await getSpaceChannelsBatch("");
    }
}

export function getDefaultChannelsAsSelectOptions() {
    if (defaultChannelsBatch == null) {
        return [];
    }
    return defaultChannelsBatch.map((channel) => channelToSelectOption(channel));
}

export const loadChannelsAsSelectOptions = (query: string, callback: OptionsCallback) => {
    const loadChannels = async () => {
        let channels = await getSpaceChannelsBatch(query);
        callback(channels.map((channel) => channelToSelectOption(channel)));
    };
    loadChannels().catch(console.error);
};

function channelToSelectOption(channel: SpaceChannelToPickForSync): ChannelSelectOption {
    return { 
        value: channel.spaceChannelId, 
        label: channel.channelNameInSpace, 
        icon: channel.icon, 
        optionType: "Space" 
    }
}
