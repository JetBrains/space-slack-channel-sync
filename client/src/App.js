import {useEffect, useState} from "react";
import * as spaceAuth from "./spaceAuth.js";
import * as slackTeams from "./slackWorkspaces.js";
import StartPage from "./startPage.js";
import MainPage from "./mainPage.js";
import * as spaceOrg from "./spaceOrg.js";
import * as slackChannels from "./slackChannels.js";
import * as spaceChannels from "./spaceChannels.js";
import * as syncedChannels from "./syncedChannels.js";
import Icon from "./components/icon";

function App() {
    const [pageSelectorDataLoaded, setPageSelectorDataLoaded] = useState(false);

    useEffect(() => {
        const fetchPageSelectorData = async () => {
            await spaceAuth.tryFetchAlreadyIssuedUserToken();
            await loadInitialData();
            setPageSelectorDataLoaded(true);
        };

        fetchPageSelectorData().catch(console.error);
    }, []);

    return (
        <div className="app">
            <span className="app-header">Two-way sync of channels between Slack and Space</span>
            {
                pageSelectorDataLoaded
                ? <PageSelector/>
                : <Loader/>
            }
        </div>
    );
}

function Loader() {
    // TODO: use loader image instead
    return (
        <Icon name="loader" style={{alignSelf: 'center', width: '50px', height: '50px'}}/>
    );
}

function PageSelector() {
    const [showStartPage, setShowStartPage] = useState(getShowStartPage())
    if (showStartPage) {
        return (
            <StartPage onAuthorizedInSpace={() => {
                const onAuthInSpace = async () => {
                    await loadInitialData();
                    setShowStartPage(getShowStartPage());
                };
                onAuthInSpace().catch(console.error);
            }}/>
        )
    } else {
        return (
            <MainPage/>
        )
    }
}

async function loadInitialData() {
    if (spaceAuth.isUserTokenPresent()) {
        await Promise.all([
            loadInitialSpaceData(),
            loadInitialSlackData(),
            syncedChannels.retrieveSyncedChannels(),
        ]);
    }
}

async function loadInitialSpaceData() {
    await Promise.all([
        spaceOrg.loadOrgName(),
        spaceChannels.retrieveDefaultChannelBatch()
    ]);
}

async function loadInitialSlackData() {
    // can't do these in parallel, the second one needs the results from the first
    await slackTeams.loadSlackWorkspaces();
    let selectedWorkspace = slackTeams.getSelectedSlackWorkspace();
    if (selectedWorkspace !== null) {
        await slackChannels.retrieveDefaultChannelBatch(selectedWorkspace.id);
    }
}

function getShowStartPage() {
    return !spaceAuth.isUserTokenPresent() || !slackTeams.isSlackWorkspaceAdded();
}

export default App;
