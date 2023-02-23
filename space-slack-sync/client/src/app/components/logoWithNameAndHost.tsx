import React, { ReactElement } from 'react';
import { JsxElement } from 'typescript';
import './logoWithNameAndHost.css';

export interface LogoWithNameAndHostProps {
    name: string | null;
    host: string | null;
    renderLogo: () => ReactElement;
}

export default function LogoWithNameAndHost(props: LogoWithNameAndHostProps) {
    return <>
        <div className="logoWithNameAndHost">
            <>
                {props.renderLogo()}
                <div className="nameAndHost">
                    <span className="logoName">{props.name}</span>
                    <span className="logoHost">{props.host}</span>
                </div>
            </>
        </div>
    </>
}
