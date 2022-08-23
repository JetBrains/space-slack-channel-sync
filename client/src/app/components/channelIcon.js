import './channelIcon.css';
import Icon from "./icon";

export default function ChannelIcon(props) {
    return (
        <div className="channelIconContainer">
            {
                props.iconUrl != null
                    ? <img className="channelIcon" src={props.iconUrl} alt=""/>
                    : <EmptyIcon
                        optionType={props.optionType}
                    />
            }
        </div>
    );
}

function EmptyIcon(props) {
    if (props.optionType === "Slack") {
        return (
            <Icon name="slack-channel" specialIconForDarkTheme={true}/>
        );
    } else {
        return (
            <Icon name="space-channel" specialIconForDarkTheme={true}/>
        );
    }
}
