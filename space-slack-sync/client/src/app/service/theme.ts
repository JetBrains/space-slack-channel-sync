import { ThemeProperties } from "../space/space";

export let isDark: boolean | null = null;
export let cssVars: Map<string, string> = new Map();

export async function initCssVars() {
    // subscribe to the changes in theme css variables
    window.addEventListener("message", (e) => {
        if (e.data.properties !== undefined && e.data.type == `ThemeProperties`) {
            const themeProperties: ThemeProperties = e.data;
            applyCssVars(themeProperties);
        }
    });

    const themeCssVars = await getCssVarsAndSubscribeForChanges();
    applyCssVars(themeCssVars);
}

function applyCssVars(themeProperties: ThemeProperties) {
    isDark = themeProperties.isDark;
    let newCssVars = new Map();
    themeProperties.properties.forEach(cssVar => {
        document.documentElement.style.setProperty(cssVar.name, cssVar.value);
        newCssVars.set(cssVar.name, cssVar.value);
    })
    cssVars = newCssVars;
}

function getCssVarsAndSubscribeForChanges(): Promise<ThemeProperties> {
    return new Promise((resolve) => {
        const channel = new MessageChannel();
        channel.port1.onmessage = e => resolve(e.data);
        window.parent.postMessage({ type: "GetThemePropertiesRequest", subscribeForUpdates: true }, "*", [channel.port2]);
    });
}
