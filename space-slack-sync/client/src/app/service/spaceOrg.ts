import {fetchFromSpace} from "./fetch";

let spaceOrg: SpaceOrganization | null = null;

interface SpaceOrganization {
    name: string;
}

export async function loadOrgName() {
    spaceOrg = await fetchFromSpace("/api/http/organization");
}

export function getOrgName(): string | null {
    return spaceOrg?.name || null;
}
