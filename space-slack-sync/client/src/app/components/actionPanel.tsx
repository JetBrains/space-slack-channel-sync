import React from 'react';
import './actionPanel.css';
import Button from "./button";

export interface ActionPanelProps {
    isActionComplete: boolean;
    headerText: string;
    buttonText: string;
    description: string;
    isDisabled: boolean;
    actionHandler: () => void;
}

export default function ActionPanel(props: ActionPanelProps) {
    let classNames = props.isActionComplete ? "actionPanelInner actionPanelInnerActionComplete" : "actionPanelInner actionPanelInnerActionNotComplete";
    return <>
        <div className="actionPanel">
            <div className={classNames}>
                <span className="actionPanelHeader">{props.headerText}</span>
                <span className="actionPanelDescription">{props.description}</span>
                {
                    !props.isActionComplete
                        ? <Button buttonText={props.buttonText} actionHandler={props.actionHandler}
                                  isDisabled={props.isDisabled}/>
                        : <AuthorizedWithCheckmark/>
                }
            </div>
        </div>
    </>
}

function AuthorizedWithCheckmark() {
    return (
        <div className="authorizedLabelDiv">
            <svg width="22" height="17" viewBox="0 0 22 17" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path
                    d="M7 16.7676L0.116211 9.88379L1.88379 8.11621L7 13.2324L20.1162 0.116211L21.8838 1.88379L7 16.7676Z"
                    fill="#4DBB5F"/>
            </svg>

            <span className="authorizedLabel">Authorized</span>
        </div>
    );
}
