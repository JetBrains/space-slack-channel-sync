import {fetchFromServer} from "./fetch";
import * as utils from "./utils";

interface SlackWorkspacesResponse {
    readonly workspaces: SlackWorkspace[];
    readonly canManage: boolean;
}

export interface SlackWorkspace {
    readonly id: string;
    readonly domain: string;
    readonly name: string;
}

let selectedSlackWorkspace: SlackWorkspace | null = null;
let slackWorkspaces: SlackWorkspace[] = [];

export async function loadSlackWorkspaces() {
    let response: SlackWorkspacesResponse = await fetchFromServer("/api/slack-workspaces");
    slackWorkspaces = response.workspaces;
    if (slackWorkspaces.length > 0) {
        selectedSlackWorkspace = slackWorkspaces[0];
    }
}

export function isSlackWorkspaceAdded(): boolean {
    return slackWorkspaces.length > 0;
}

export function addSlackWorkspace() {
    const addWorkspace = async () => {
        let installToSlackUrl: string = await fetchFromServer("/api/url-for-adding-slack-team", 'GET', true);
        utils.redirectTopWindow(await installToSlackUrl);
    };
    addWorkspace().catch(console.error);
}

export function getSelectedSlackWorkspace(): SlackWorkspace | null {
    return selectedSlackWorkspace;
}
