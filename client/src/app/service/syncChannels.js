import fetchFromServer from "./fetch";
import * as slackWorkspaces from "./slackTeams.js";
import * as slackChannels from "./slackChannels";
import * as spaceChannels from "./spaceChannels";
import * as slackTeams from "./slackTeams";
import * as syncedChannels from "./syncedChannels.js";

export let chosenSlackChannelToSync = null;
export let chosenSpaceChannelToSync = null;

export function setSlackChannelToSync(slackChannelId) {
    chosenSlackChannelToSync = slackChannelId;
}

export function setSpaceChannelToSync(spaceChannelId) {
    chosenSpaceChannelToSync = spaceChannelId;
}

export function readyToSync() {
    return chosenSlackChannelToSync != null && chosenSpaceChannelToSync != null;
}

export function startSync(setChannels) {
    let slackTeam = slackWorkspaces.getSelectedSlackWorkspace();
    const startSyncCall = async () => {
        await fetchFromServer(
            `/api/start-sync?spaceChannelId=${chosenSpaceChannelToSync}&slackTeamId=${slackTeam.id}&slackChannelId=${chosenSlackChannelToSync}`,
            'POST'
        );
        await refreshChannelData(setChannels);
    };
    startSyncCall().catch(console.error);
}

export function removeChannelFromSync(syncedChannel, setChannels) {
    const removeCall = async () => {
        await fetchFromServer(
            `/api/stop-sync?spaceChannelId=${syncedChannel.spaceChannelId}&slackTeamId=${syncedChannel.slackTeamId}&slackChannelId=${syncedChannel.slackChannelId}`,
            `POST`
        );
        await refreshChannelData(setChannels);
    };
    removeCall().catch(console.error);
}

async function refreshChannelData(setChannels) {
    await Promise.all([
        spaceChannels.retrieveDefaultChannelBatch(true),
        slackChannels.retrieveDefaultChannelBatch(slackTeams.getSelectedSlackWorkspace().id, true),
        syncedChannels.retrieveSyncedChannels(),
    ]);
    setChannels(syncedChannels.getSyncedChannels());
}
