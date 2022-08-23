import {useEffect, useState} from "react";
import * as spaceAuth from "./service/spaceAuth.js";
import * as slackTeams from "./service/slackTeams.js";
import StartPage from "./components/startPage.js";
import MainPage from "./components/mainPage.js";
import * as spaceOrg from "./service/spaceOrg.js";
import * as slackChannels from "./service/slackChannels.js";
import * as spaceChannels from "./service/spaceChannels.js";
import * as syncedChannels from "./service/syncedChannels.js";
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
            <span className="app-header">Two-way sync of messages between Slack and Space</span>
            {
                pageSelectorDataLoaded
                ? <PageSelector/>
                : <Loader/>
            }
        </div>
    );
}

function Loader() {
    return (
        <Icon name="loader" style={{alignSelf: 'center', width: '50px', height: '50px', marginTop: '50px'}}/>
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
