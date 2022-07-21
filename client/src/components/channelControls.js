import './channelControls.css';
import React, {Fragment} from 'react';
import Icon from "./icon";

export default function ChannelControls(props) {
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
            {
                props.hasPermissionToStopSync &&
                <div className="binIconContainer" onClick={() => props.onRemoveChannelFromSync()}>
                    <Icon name="delete" specialIconForDarkTheme={true}/>
                </div>
            }
        </div>
    );
}

export function TunnellingIconWithText(props) {
    return (
        <Fragment>
            <Icon name="tunnelling"/>
            <span className="tunnellingStatusSpan">{props.text}</span>
        </Fragment>
    );
}

export function PendingIconWithText(props) {
    return (
        <Fragment>
            <Icon name="pending"/>
            <span className="pendingStatusSpan">{props.text}</span>
        </Fragment>
    );
}