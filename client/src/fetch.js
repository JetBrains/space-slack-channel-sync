import * as spaceAuth from "./spaceAuth";

export default async function fetchFromServer(path, method) {
    if (!spaceAuth.isUserTokenPresent()) {
        console.error(`Request ${path} cannot be made: user token is absent`);
        return;
    }

    const httpMethod = method === undefined ? 'GET' : method;
    return await fetch(path, {method: httpMethod, headers: {"Authorization": "Bearer " + spaceAuth.getUserToken()}});
}

export async function fetchFromSpace(path) {
    let spaceServerUrl = spaceAuth.getSpaceServerUrl();
    if (spaceServerUrl == null) {
        console.error(`Request ${path} cannot be made: space server url is absent`);
        return;
    }

    return await fetch(spaceServerUrl + path, {headers: {"Authorization": "Bearer " + spaceAuth.getUserToken()}});
}
