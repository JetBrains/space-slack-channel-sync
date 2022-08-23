export function approveChannelPermissions(spaceChannelId, onSuccess) {
    const asyncRequestAndFetch = async () => {
        await approveChannelPermissionsImpl(spaceChannelId, onSuccess);
    };

    asyncRequestAndFetch().catch(console.error);
}

async function approveChannelPermissionsImpl(spaceChannelId, onSuccess) {
    const scope = `channel:${spaceChannelId}:Channel.ViewChannel channel:${spaceChannelId}:Channel.ViewMessages channel:${spaceChannelId}:Channel.ImportMessages`

    let response = await new Promise((resolve) => {
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
