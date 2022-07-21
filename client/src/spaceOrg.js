import {fetchFromSpace} from "./fetch";

let spaceOrgName = null;

export async function loadOrgName() {
    let response = await fetchFromSpace("/api/http/organization");
    spaceOrgName = (await response.json()).name;
}

export function getOrgName() {
    return spaceOrgName;
}
