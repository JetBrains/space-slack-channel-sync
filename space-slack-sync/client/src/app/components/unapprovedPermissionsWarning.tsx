import { useEffect, useState } from "react";
import WarningBox from "./warningBox";
import * as permissions from "../service/permissions";
import React from "react";

export default function UnapprovedPermissionsWarning() {
    let [missingPermissions, setMissingPermissions] = useState<string | null>(null);
    let [hasPermissionsToApprove, setHasPermissionsToApprove] = useState<boolean | null>(null);

    useEffect(() => {
        permissions.getMissingPermissions((missingPermissions, hasPermissionsToApprove) => {
            setHasPermissionsToApprove(hasPermissionsToApprove);
            setMissingPermissions(missingPermissions);
        });
    }, []);

    if (missingPermissions == null) {
        return (
            <div style={{ height: '60px' }} />
        );
    }

    let onApprove = () => {
        setMissingPermissions(null)
    };

    let warningText = hasPermissionsToApprove ? "Please approve \"View Profiles\" permission for the application" : "Space administrator needs to approve \"View Profiles\" permission for the application.";
    return <WarningBox isActionable={hasPermissionsToApprove == true}
        text={warningText}
        onAction={() => {
            if (missingPermissions != null) {
                permissions.approvePermissions(missingPermissions, onApprove)
            }
        }}
        style={{ alignSelf: 'stretch', marginTop: '32px' }}
    />
}
