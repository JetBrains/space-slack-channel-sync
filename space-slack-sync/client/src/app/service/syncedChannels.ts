import { fetchFromServer } from "./fetch";

let syncedChannels: SyncedChannelInfo[] = [];

interface ListSyncedChannelsResponse {
    syncedChannels: SyncedChannelInfo[];
}

export interface SyncedChannelInfo {
    channelNameInSpace: string;
    channelNameInSlack: string
    spaceChannelId: string;
    spaceChannelIconUrl: string | null;
    slackTeamId: string;
    slackChannelId: string;
    isMemberInSlackChannel: boolean,
    isAuthorizedInSpaceChannel: boolean;
    userIsAdminInSpaceChannel: boolean;
    isNewChannelForOptimisticUpdate: boolean;
}

export async function retrieveSyncedChannels() {
    syncedChannels = await getSyncedChannelsFromServer();
}

export function getSyncedChannels(): SyncedChannelInfo[] {
    return syncedChannels;
}

async function getSyncedChannelsFromServer(): Promise<SyncedChannelInfo[]> {
    let response: ListSyncedChannelsResponse = await fetchFromServer(`/api/synced-channels`);
    return response.syncedChannels;
}

export function refreshSyncedChannels(setChannels: (channels: SyncedChannelInfo[]) => void) {
    const refreshCall = async () => {
        await retrieveSyncedChannels();
        setChannels(syncedChannels)
    };
    refreshCall().catch(console.error);
}
