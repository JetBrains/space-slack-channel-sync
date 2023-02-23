export interface ApprovePermissionsResponse {
    readonly success: boolean;
    readonly message: string;
}

export interface ThemeProperty {
    readonly name: string;
    readonly value: string;
}

export interface ThemeProperties {
    readonly properties: Array<ThemeProperty>;
    readonly type: string;
    readonly isDark: boolean;
}
