import './channelControls.css';
import React, { Fragment } from 'react';
import Icon from "./icon";
import Spring from "./spring";

export interface ChannelControlsProps {
    synced: boolean;
    hasPermissionToStopSync: boolean;
    onRemoveChannelFromSync: () => void;
}

export default function ChannelControls(props: ChannelControlsProps) {
    return (
        <div className="channelControlContainer">
            {
                props.synced
                    ? <TunnellingIconWithText
                        text="Tunnelling"
                    />
                    : <PendingIconWithText
                        text="Pending"
                    />
            }
            <Spring/>
            {
                props.hasPermissionToStopSync &&
                <div className="binIconContainer" onClick={() => props.onRemoveChannelFromSync()}>
                    <Icon name="delete" specialIconForDarkTheme={true} />
                </div>
            }
        </div>
    );
}

export interface IconWithTextProps {
    text: string;
}

export function TunnellingIconWithText(props: IconWithTextProps) {
    return (
        <Fragment>
            <Icon name="tunnelling" />
            <span className="tunnellingStatusSpan">{props.text}</span>
        </Fragment>
    );
}

export function PendingIconWithText(props: IconWithTextProps) {
    return (
        <>
            <Icon name="pending" />
            <span className="pendingStatusSpan">{props.text}</span>
        </>
    );
}
