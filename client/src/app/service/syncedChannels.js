import fetchFromServer from "./fetch";

let syncedChannels = null;

export async function retrieveSyncedChannels() {
    syncedChannels = await getSyncedChannelsFromServer();
}

export function getSyncedChannels() {
    return syncedChannels;
}

async function getSyncedChannelsFromServer() {
    let response = await fetchFromServer(`/api/synced-channels`);
    return (await response.json()).syncedChannels;
}

export function refreshSyncedChannels(setChannels) {
    const refreshCall = async () => {
        await retrieveSyncedChannels();
        setChannels(syncedChannels)
    };
    refreshCall().catch(console.error);
}
