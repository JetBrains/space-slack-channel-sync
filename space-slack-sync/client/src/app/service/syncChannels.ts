import { fetchFromServer } from "./fetch";
import * as slackWorkspaces from "./slackTeams";
import * as slackChannels from "./slackChannels";
import * as spaceChannels from "./spaceChannels";
import * as slackTeams from "./slackTeams";
import * as syncedChannels from "./syncedChannels";
import { SyncedChannelInfo } from "./syncedChannels";

export let chosenSlackChannelToSync: string | null = null;
export let chosenSpaceChannelToSync: string | null = null;

export function setSlackChannelToSync(slackChannelId: string) {
    chosenSlackChannelToSync = slackChannelId;
}

export function setSpaceChannelToSync(spaceChannelId: string) {
    chosenSpaceChannelToSync = spaceChannelId;
}

export function readyToSync(): boolean {
    return chosenSlackChannelToSync != null && chosenSpaceChannelToSync != null;
}

export function startSync(setChannels: (channels: SyncedChannelInfo[]) => void) {
    const slackWorkspace = slackWorkspaces.getSelectedSlackWorkspace();
    if (slackWorkspace == null) {
        throw `Slack workspace is not defined`;
    }

    const startSyncCall = async () => {
        await fetchFromServer(
            `/api/start-sync?spaceChannelId=${chosenSpaceChannelToSync}&slackTeamId=${slackWorkspace.id}&slackChannelId=${chosenSlackChannelToSync}`,
            'POST'
        );
        await refreshChannelData(setChannels);
    };
    startSyncCall().catch(console.error);
}

export function removeChannelFromSync(syncedChannel: SyncedChannelInfo, setChannels: (channels: SyncedChannelInfo[]) => void) {
    const removeCall = async () => {
        await fetchFromServer(
            `/api/stop-sync?spaceChannelId=${syncedChannel.spaceChannelId}&slackTeamId=${syncedChannel.slackTeamId}&slackChannelId=${syncedChannel.slackChannelId}`,
            `POST`
        );
        await refreshChannelData(setChannels);
    };
    removeCall().catch(console.error);
}

async function refreshChannelData(setChannels: (channels: SyncedChannelInfo[]) => void) {
    const slackWorkspace = slackTeams.getSelectedSlackWorkspace();
    await Promise.all([
        spaceChannels.retrieveDefaultChannelBatch(true),
        slackWorkspace == null || slackChannels.retrieveDefaultChannelBatch(slackWorkspace.id, true),
        syncedChannels.retrieveSyncedChannels(),
    ]);
    setChannels(syncedChannels.getSyncedChannels());
}
