export function redirectTopWindow(redirectUrl) {
    const channel = new MessageChannel();
    window.parent.postMessage({
        type: "RedirectWithConfirmationRequest",
        redirectUrl: redirectUrl
    }, "*", [channel.port2]);
}

export function debounce(func, timeout = 300) {
    let timer;
    return (...args) => {
        clearTimeout(timer);
        timer = setTimeout(() => {
            func.apply(this, args);
        }, timeout);
    };
}

export function redirectToSlackChannel(slackChannelId) {
    openInNewTab(`https://slack.com/app_redirect?channel=${slackChannelId}`);
}

function openInNewTab(href) {
    Object.assign(document.createElement('a'), {
        target: '_blank',
        rel: 'noopener noreferrer',
        href: href,
    }).click();
}
