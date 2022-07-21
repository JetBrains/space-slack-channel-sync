let userToken = null;
let spaceServerUrl = null;
let spaceDomain = null;
let onAuthorizedInSpace = () => {};

export async function tryFetchAlreadyIssuedUserToken() {
    return getUserAccessTokenImpl(false);
}

export function requestAndFetchUserAccessToken() {
    const asyncRequestAndFetch = async () => {
        await getUserAccessTokenImpl(true);
    };

    asyncRequestAndFetch().catch(console.error);
}

async function getUserAccessTokenImpl(askForConsent) {
    let tokenResponse = await new Promise((resolve) => {
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

export function isUserTokenPresent() {
    return userToken != null;
}

export function getUserToken() {
    // TODO: check for expiration
    return userToken;
}

export function getSpaceServerUrl() {
    return spaceServerUrl;
}

export function getSpaceDomain() {
    return spaceDomain;
}

export function setUserToken(newUserToken) {
    userToken = newUserToken;
    onAuthorizedInSpace();
}

export function setOnAuthorizedInSpaceCallback(callback) {
    onAuthorizedInSpace = callback;
}
