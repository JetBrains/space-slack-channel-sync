import { fetchFromServer } from "./fetch";
import { ApprovePermissionsResponse } from "../space/space";

interface MissingAppPermissionsResponse {
    readonly missingPermissions: string | null;
    readonly hasPermissionsToApprove: boolean;
}

export function getMissingPermissions(setResult: (missingPermissions: string | null, hasPermissionsToApprove: boolean) => void) {
    const call = async () => {
        let response: MissingAppPermissionsResponse = await fetchFromServer("/api/missing-app-permissions");
        setResult(response.missingPermissions, response.hasPermissionsToApprove);
    };
    call().catch(console.error);
}

interface ApprovePermissionsRequest {
    permissionScope: string;
    unfurlDomains: string;
    unfurlPatterns: string;
    purpose: string;
}

export function approvePermissions(permissionScope: string, onApproved: () => void) {
    let call = async () => {
        console.log(`Calling approvePermissions, permissions = ${permissionScope}`)
        let response: ApprovePermissionsResponse = await new Promise((resolve) => {
            const channel = new MessageChannel();
            channel.port1.onmessage = e => resolve(e.data);
            window.parent.postMessage({
                type: "ApprovePermissionsRequest",
                permissionScope: permissionScope,
                purpose: ""
            }, "*", [channel.port2]);
        });

        if (response.success) {
            onApproved()
        }
    };
    call().catch(console.error);
}
