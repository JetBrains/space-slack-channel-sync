import './channelIcon.css';
import Icon from "./icon";
import React, {Fragment} from 'react';

export interface ChannelIconProps {
    iconUrl: string | null;
    optionType: string;
}

export default function ChannelIcon(props: ChannelIconProps) {
    return <>
        <div className="channelIconContainer">
            {
                props.iconUrl != null
                    ? <img className="channelIcon" src={props.iconUrl} alt=""/>
                    : <EmptyIcon
                        optionType={props.optionType}
                    />
            }
        </div>
        </>
}

interface EmptyIconProps {
    optionType: string;
}

function EmptyIcon(props: EmptyIconProps) {
    if (props.optionType === "Slack") {
        return <>
            <Icon name="slack-channel" specialIconForDarkTheme={true}/>
            </>
    } else {
        return <>
            <Icon name="space-channel" specialIconForDarkTheme={true}/>
        </>
    }
}
