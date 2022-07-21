import fetchFromServer from "./fetch";

let defaultChannelsBatch = null;

async function getSpaceChannelsBatch(query) {
    let response = await fetchFromServer(`/api/space-channels-to-pick-for-sync?query=${query}`);
    return (await response.json()).spaceChannelsToPickForSync;
}

export async function retrieveDefaultChannelBatch(recalc) {
    if (defaultChannelsBatch == null || recalc) {
        defaultChannelsBatch = await getSpaceChannelsBatch("");
    }
}

export function getDefaultChannelsAsSelectOptions() {
    return defaultChannelsBatch.map((channel) => channelToSelectOption(channel));
}

export const loadChannelsAsSelectOptions = (query, callback) => {
    const loadChannels = async () => {
        let channels = await getSpaceChannelsBatch(query);
        callback(channels.map((channel) => channelToSelectOption(channel)));
    };
    loadChannels().catch(console.error);
};

function channelToSelectOption(channel) {
    return {value: channel.spaceChannelId, label: channel.channelNameInSpace, icon: channel.icon, optionType: "Space"}
}
