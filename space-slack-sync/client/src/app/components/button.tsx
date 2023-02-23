import './button.css';
import * as theme from "../service/theme";
import React from 'react';

export interface ButtonProps {
    isDisabled: boolean;
    actionHandler: () => void;
    buttonText: string;
}

export default function Button(props: ButtonProps) {
    let disabledClassName = theme.isDark ? "buttonDisabledDark" : "buttonDisabledLight";
    let enabledClassName = theme.isDark ? "buttonEnabledDark" : "buttonEnabledLight"
    let classNames = props.isDisabled ? `button ${disabledClassName}` : `button buttonEnabled ${enabledClassName}`;
    return <>
        <button
            className={classNames}
            type="button"
            onClick={() => {
                if (!props.isDisabled) {
                    props.actionHandler()
                }
            }}
        >
            <span className="buttonText">{props.buttonText}</span>
        </button>
    </>
}
