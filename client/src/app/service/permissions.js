import fetchFromServer from "./fetch";

export function getMissingPermissions(setResult) {
    const call = async () => {
        let response = await fetchFromServer("/api/missing-app-permissions");
        let missingPermissionsResponse = await response.json();
        setResult(missingPermissionsResponse.missingPermissions, missingPermissionsResponse.hasPermissionsToApprove);
    };
    call().catch(console.error);
}

export function approvePermissions(permissions, onApproved) {
    let call = async () => {
        console.log(`Calling approvePermissions, permissions = ${permissions}`)
        let response = await new Promise((resolve) => {
            const channel = new MessageChannel();
            channel.port1.onmessage = e => resolve(e.data);
            window.parent.postMessage({
                type: "ApprovePermissionsRequest",
                permissionScope: permissions,
                purpose: ""
            }, "*", [channel.port2]);
        });

        if (response.success) {
            onApproved()
        }
    };
    call().catch(console.error);
}
