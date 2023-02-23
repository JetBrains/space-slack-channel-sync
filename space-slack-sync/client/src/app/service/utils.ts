export function redirectTopWindow(redirectUrl: string) {
    const channel = new MessageChannel();
    window.parent.postMessage({
        type: "RedirectWithConfirmationRequest",
        redirectUrl: redirectUrl
    }, "*", [channel.port2]);
}

export function debounce<T extends Function>(cb: T, wait = 200) {
    let timer: NodeJS.Timer;
    let callable = (...args: any) => {
        clearTimeout(timer);
        timer = setTimeout(() => cb(...args), wait);
    };
    return <T>(<any>callable);
}

// export function debounce(func: Function, timeout: number = 300) {
//     let timer: NodeJS.Timer;
//     return (...args: any) => {
//         clearTimeout(timer);
//         timer = setTimeout(() => {
//             func.apply(this, args);
//         }, timeout);
//     };
// }

export function redirectToSlackChannel(slackChannelId: string) {
    openInNewTab(`https://slack.com/app_redirect?channel=${slackChannelId}`);
}

function openInNewTab(href: string) {
    Object.assign(document.createElement('a'), {
        target: '_blank',
        rel: 'noopener noreferrer',
        href: href,
    }).click();
}
