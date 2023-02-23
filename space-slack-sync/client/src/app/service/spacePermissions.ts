import { ApprovePermissionsResponse } from "../space/space";

export function approveChannelPermissions(spaceChannelId: string, onSuccess: () => void) {
    const asyncRequestAndFetch = async () => {
        await approveChannelPermissionsImpl(spaceChannelId, onSuccess);
    };

    asyncRequestAndFetch().catch(console.error);
}

async function approveChannelPermissionsImpl(spaceChannelId: string, onSuccess: () => void) {
    const scope = `channel:${spaceChannelId}:Channel.ViewChannel channel:${spaceChannelId}:Channel.ViewMessages channel:${spaceChannelId}:Channel.ImportMessages`

    let response: ApprovePermissionsResponse = await new Promise((resolve) => {
        const channel = new MessageChannel();
        channel.port1.onmessage = e => resolve(e.data);
        window.parent.postMessage({
            type: "ApprovePermissionsRequest",
            permissionScope: scope,
        }, "*", [channel.port2]);
    });

    if (response.success === true) {
        onSuccess()
    }
}
