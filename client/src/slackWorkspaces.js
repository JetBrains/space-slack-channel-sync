import fetchFromServer from "./fetch.js";
import * as utils from "./utils.js";

let selectedSlackWorkspace = null;
let slackWorkspaces = [];

export async function loadSlackWorkspaces() {
    let response = await fetchFromServer("/api/slack-workspaces");
    slackWorkspaces = (await response.json()).workspaces;
    if (slackWorkspaces.length > 0) {
        selectedSlackWorkspace = slackWorkspaces[0];
    }
}

export function isSlackWorkspaceAdded() {
    return slackWorkspaces.length > 0;
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
