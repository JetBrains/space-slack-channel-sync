import './channelIcon.css';
import * as theme from "../theme";

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
            <svg className="channelIcon" width="16" height="16" viewBox="0 0 16 16" fill="none"
                 xmlns="http://www.w3.org/2000/svg">
                <path
                    d="M5 0C5.55229 0 6 0.447715 6 1V4L10 4V1C10 0.447715 10.4477 0 11 0C11.5523 0 12 0.447715 12 1V4H15C15.5523 4 16 4.44771 16 5C16 5.55228 15.5523 6 15 6H12V10H15C15.5523 10 16 10.4477 16 11C16 11.5523 15.5523 12 15 12H12V15C12 15.5523 11.5523 16 11 16C10.4477 16 10 15.5523 10 15V12L6 12V15C6 15.5523 5.55229 16 5 16C4.44771 16 4 15.5523 4 15V12H1C0.447715 12 0 11.5523 0 11C0 10.4477 0.447715 10 1 10H4V6H1C0.447715 6 0 5.55229 0 5C0 4.44771 0.447715 4 1 4H4V1C4 0.447715 4.44771 0 5 0ZM6 6V10L10 10V6L6 6Z"
                    fill={theme.cssVars['--text-color']}/>
            </svg>
        );
    } else {
        const fill = theme.isDark ? "#FFFFFF" : "#0C0C0D";
        const fillOpacity = theme.isDark ? "0.14" : "0.07";
        return (
            <svg className="channelIcon" width="32" height="32" viewBox="0 0 32 32" fill="none"
                 xmlns="http://www.w3.org/2000/svg">
                <circle cx="16" cy="16" r="16" fill={fill} fillOpacity={fillOpacity}/>
                <path
                    d="M14 10.5C14.2761 10.5 14.5 10.7239 14.5 11V13.5H17.5V11C17.5 10.7239 17.7239 10.5 18 10.5C18.2761 10.5 18.5 10.7239 18.5 11V13.5H20.5C20.7761 13.5 21 13.7239 21 14C21 14.2761 20.7761 14.5 20.5 14.5H18.5V17.5H20.5C20.7761 17.5 21 17.7239 21 18C21 18.2761 20.7761 18.5 20.5 18.5H18.5V21C18.5 21.2761 18.2761 21.5 18 21.5C17.7239 21.5 17.5 21.2761 17.5 21V18.5H14.5V21C14.5 21.2761 14.2761 21.5 14 21.5C13.7239 21.5 13.5 21.2761 13.5 21V18.5H11.5C11.2239 18.5 11 18.2761 11 18C11 17.7239 11.2239 17.5 11.5 17.5H13.5V14.5H11.5C11.2239 14.5 11 14.2761 11 14C11 13.7239 11.2239 13.5 11.5 13.5H13.5V11C13.5 10.7239 13.7239 10.5 14 10.5ZM14.5 14.5V17.5H17.5V14.5H14.5Z"
                    fill={theme.cssVars['--text-color']}/>
            </svg>
        )
    }
}
