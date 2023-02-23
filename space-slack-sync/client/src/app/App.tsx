import './App.css';

import {useEffect, useState} from "react";
import * as spaceAuth from "./service/spaceAuth";
import * as slackTeams from "./service/slackTeams";
import StartPage from "./components/startPage";
import MainPage from "./components/mainPage";
import * as spaceOrg from "./service/spaceOrg";
import * as slackChannels from "./service/slackChannels";
import * as spaceChannels from "./service/spaceChannels";
import * as syncedChannels from "./service/syncedChannels";
import Icon from "./components/icon";
import React from 'react';
import { SlackWorkspace } from './service/slackTeams';

export const App = () => {
    const [pageSelectorDataLoaded, setPageSelectorDataLoaded] = useState(false);

    useEffect(() => {
        const fetchPageSelectorData = async () => {
            await spaceAuth.tryFetchAlreadyIssuedUserToken();
            await loadInitialData();
            setPageSelectorDataLoaded(true);
        };

        fetchPageSelectorData().catch(console.error);
    }, []);

    return <>
        <div className="app">
            <span className="app-header">Two-way sync of messages between Slack and Space</span>
            {
                pageSelectorDataLoaded
                ? <PageSelector/>
                : <Loader/>
            }
        </div>
    </>
}

function Loader() {
    return (
        <Icon name="loader" style={{alignSelf: 'center', width: '50px', height: '50px', marginTop: '50px'}}/>
    );
}

function PageSelector() {
    const [isUserTokenPresent, setIsUserTokenPresent] = useState<boolean>(spaceAuth.isUserTokenPresent());
    const [slackWorkspace, setSlackWorkspace] = useState<SlackWorkspace | null>(slackTeams.getSelectedSlackWorkspace());

    if (!isUserTokenPresent || slackWorkspace == null) {
        return (
            <StartPage onAuthorizedInSpace={() => {
                const onAuthInSpace = async () => {
                    await loadInitialData();
                    setIsUserTokenPresent(spaceAuth.isUserTokenPresent());
                    setSlackWorkspace(slackTeams.getSelectedSlackWorkspace());
                };
                onAuthInSpace().catch(console.error);
            }}/>
        )
    } else {
        return (
            <MainPage slackWorkspace={slackWorkspace}/>
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
        spaceChannels.retrieveDefaultChannelBatch(false)
    ]);
}

async function loadInitialSlackData() {
    // can't do these in parallel, the second one needs the results from the first
    await slackTeams.loadSlackWorkspaces();
    let selectedWorkspace = slackTeams.getSelectedSlackWorkspace();
    if (selectedWorkspace !== null) {
        await slackChannels.retrieveDefaultChannelBatch(selectedWorkspace.id, false);
    }
}

function getShowStartPage() {
    return !spaceAuth.isUserTokenPresent() || !slackTeams.isSlackWorkspaceAdded();
}

export default App;
