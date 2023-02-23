let userToken: string | null = null;
let spaceServerUrl: string | null = null;
let spaceDomain: string | null = null;
let onAuthorizedInSpace: () => void = () => {};

export async function tryFetchAlreadyIssuedUserToken(): Promise<string | null> {
    return getUserAccessTokenImpl(false);
}

export function requestAndFetchUserAccessToken() {
    const asyncRequestAndFetch = async () => {
        await getUserAccessTokenImpl(true);
    };

    asyncRequestAndFetch().catch(console.error);
}

interface GetUserTokenResponse {
    token: string;
    serverUrl: string;
}

async function getUserAccessTokenImpl(askForConsent: boolean): Promise<string | null> {
    let tokenResponse: GetUserTokenResponse = await new Promise((resolve) => {
        const channel = new MessageChannel();
        channel.port1.onmessage = e => resolve(e.data);
        window.parent.postMessage({
            type: "GetUserTokenRequest",
            permissionScope: "global:Chat.BrowseChannels global:Channel.ViewChannel",
            askForConsent: askForConsent
        }, "*", [channel.port2]);
    });


    if (tokenResponse != null && tokenResponse.token != null) {
        spaceServerUrl = tokenResponse.serverUrl;
        spaceDomain = new URL(spaceServerUrl).host;
        setUserToken(tokenResponse.token);
        return tokenResponse.token;
    }

    return null;
}

export function isUserTokenPresent(): boolean {
    return userToken != null;
}

export function getUserToken(): string | null {
    // TODO: check for expiration
    return userToken;
}

export function getSpaceServerUrl(): string | null {
    return spaceServerUrl;
}

export function getSpaceDomain(): string | null {
    return spaceDomain;
}

function setUserToken(newUserToken: string) {
    userToken = newUserToken;
    onAuthorizedInSpace();
}

export function setOnAuthorizedInSpaceCallback(callback: () => void) {
    onAuthorizedInSpace = callback;
}
