import fetchFromServer from "./fetch.js";
import * as utils from "./utils.js";

let selectedSlackWorkspace = null;
let slackTeams = [];

export async function loadSlackWorkspaces() {
    let response = await fetchFromServer("/api/slack-workspaces");
    slackTeams = (await response.json()).workspaces;
    if (slackTeams.length > 0) {
        selectedSlackWorkspace = slackTeams[0];
    }
}

export function isSlackWorkspaceAdded() {
    return slackTeams.length > 0;
}

export function addSlackWorkspace() {
    const addWorkspace = async () => {
        let response = await fetchFromServer("/api/url-for-adding-slack-team");
        utils.redirectTopWindow(await response.text());
    };
    addWorkspace().catch(console.error);
}

export function getSelectedSlackWorkspace() {
    return selectedSlackWorkspace;
}
