import * as spaceAuth from "./spaceAuth";

export async function fetchFromServer<T>(path: string, method: string = 'GET', stringResponse: boolean = false): Promise<T> {
    if (!spaceAuth.isUserTokenPresent()) {
        throw `Request ${path} cannot be made: user token is absent`;
    }

    const rawResponse = await fetch(path, { method: method, headers: { "Authorization": "Bearer " + spaceAuth.getUserToken() } });
    if (stringResponse) {
        return await rawResponse.text() as T;
    } else {
        return await rawResponse.json();
    }
}

export async function fetchFromSpace<T>(path: string): Promise<T> {
    let spaceServerUrl = spaceAuth.getSpaceServerUrl();
    if (spaceServerUrl == null) {
        throw `Request ${path} cannot be made: space server url is absent`;
    }

    const response = await fetch(spaceServerUrl + path, { headers: { "Authorization": "Bearer " + spaceAuth.getUserToken() } });
    return response.json();
}
