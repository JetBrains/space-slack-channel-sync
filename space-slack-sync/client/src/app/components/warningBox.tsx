import "./warningBox.css";
import Icon from "./icon";
import React from "react";

export interface WarningBoxProps {
    text: string;
    style: object;
    isActionable: boolean;
    onAction: () => void;
}

export default function WarningBox(props: WarningBoxProps) {
    let spanClassNames = props.isActionable ? "warning-span warning-span-actionable" : "warning-span";
    return <div className="warning-box" style={props.style}>
        <div className="warning-content-container">
            <Icon name="warning" style={{ marginRight: '10px' }} />
            <span className={spanClassNames} onClick={() => warningTextClicked(props)}>{props.text}</span>
        </div>
    </div>
}

function warningTextClicked(props: WarningBoxProps) {
    if (props.isActionable) {
        props.onAction();
    }
}
