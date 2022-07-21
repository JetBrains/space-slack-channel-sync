import {useState} from "react";
import ActionPanel from "./components/actionPanel";
import * as slackWorkspaces from "./slackWorkspaces.js";
import * as spaceAuth from "./spaceAuth.js";

export default function StartPage(props) {
    let [authorizedInSpace, setAuthorizedInSpace] = useState(spaceAuth.isUserTokenPresent());
    spaceAuth.setOnAuthorizedInSpaceCallback(() => {
        setAuthorizedInSpace(true);
        props.onAuthorizedInSpace();
    });

    return (
        <div className="startPage">
            <span className="sub-header">Before you start</span>
            <div className="actionPanelContainer">
                <ActionPanel
                    headerText="Authorize in Space"
                    description="Authorize the application to view channels in Space on your behalf"
                    buttonText="Authorize"
                    isActionComplete={authorizedInSpace}
                    actionHandler={() => {
                        spaceAuth.requestAndFetchUserAccessToken();
                    }}
                />
                <ActionPanel
                    headerText="Add Slack workspace"
                    description="Authorize the application in your Slack workspace to start channel synchronization."
                    buttonText="Add Slack workspace"
                    isActionComplete={false}
                    actionHandler={() => {
                        slackWorkspaces.addSlackWorkspace();
                    }}
                    isDisabled = {!authorizedInSpace}
                />
            </div>
        </div>
    )
}
