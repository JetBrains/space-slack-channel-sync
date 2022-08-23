import './mainPage.css';
import LogoWithNameAndHost from "./logoWithNameAndHost";
import * as spaceOrg from "../service/spaceOrg";
import * as slackWorkspaces from "../service/slackTeams";
import * as spaceAuth from "../service/spaceAuth";
import {useState} from "react";
import React, { Fragment } from 'react'
import * as slackChannels from "../service/slackChannels";
import * as spaceChannels from "../service/spaceChannels";
import ChannelSelectField from "./select";
import * as syncChannels from "../service/syncChannels.js";
import Button from "./button";
import * as syncedChannels from "../service/syncedChannels.js";
import ChannelIcon from "./channelIcon";
import Spring from "./spring";
import Icon from "./icon";
import ChannelControls from "./channelControls";
import {redirectToSlackChannel} from "../service/utils";
import * as spacePermissions from "../service/spacePermissions";
import UnapprovedPermissionsWarning from "./unapprovedPermissionsWarning";

export default function MainPage() {
    let [readyToSync, setReadyToSync] = useState(syncChannels.readyToSync());
    let [channels, setChannels] = useState(syncedChannels.getSyncedChannels());
    let [selectedSlackChannel, setSelectedSlackChannel] = useState(null);
    let [selectedSpaceChannel, setSelectedSpaceChannel] = useState(null);

    return (
        <div className="mainPage">
            <UnapprovedPermissionsWarning/>
            <div className="mainPageRow">
                <div className="mainPageCell">
                    <LogoWithNameAndHost
                        renderLogo={() => (<Icon name="slack"/>)}
                        name={slackWorkspaces.getSelectedSlackWorkspace().name}
                        host={`${slackWorkspaces.getSelectedSlackWorkspace().domain}.slack.com`}
                    />
                </div>
                <div className="mainPageCell">
                    <LogoWithNameAndHost
                        renderLogo={() => (<Icon name="space"/>)}
                        name={spaceOrg.getOrgName()}
                        host={spaceAuth.getSpaceDomain()}
                    />
                </div>
                <div className="mainPageCell"/>
            </div>

            <div className="mainPageRow">
                <div className="mainPageCell">
                    <div className="pickChannelSpan">{"Pick a Slack channel"}</div>
                </div>
                <div className="mainPageCell">
                    <div className="pickChannelSpan">{"Pick a Space channel"}</div>
                </div>
                <div className="mainPageCell"/>
            </div>

            <div className="mainPageRow selectFieldsRow">
                <div className="mainPageCell">
                    <ChannelSelectField
                        defaultOptions={slackChannels.getDefaultChannelsAsSelectOptions()}
                        loadOptions={slackChannels.loadChannelsAsSelectOptions}
                        onChange={(selectedOption) => {
                            setSelectedSlackChannel(selectedOption);
                            syncChannels.setSlackChannelToSync(selectedOption.value);
                            setReadyToSync(syncChannels.readyToSync());
                        }}
                        value={selectedSlackChannel}
                    />
                </div>
                <div className="mainPageCell">
                    <ChannelSelectField
                        defaultOptions={spaceChannels.getDefaultChannelsAsSelectOptions()}
                        loadOptions={spaceChannels.loadChannelsAsSelectOptions}
                        onChange={(selectedOption) => {
                            syncChannels.setSpaceChannelToSync(selectedOption.value);
                            setReadyToSync(syncChannels.readyToSync());
                            setSelectedSpaceChannel(selectedOption);
                        }}
                        value={selectedSpaceChannel}
                    />
                </div>
                <div className="mainPageCell">
                    <Button
                        buttonText="Create tunnel"
                        isDisabled={!readyToSync}
                        actionHandler={() => {
                            const newChannel = {
                                // optimistic update
                                channelNameInSpace: selectedSpaceChannel.label,
                                channelNameInSlack: selectedSlackChannel.label,
                                spaceChannelId: selectedSpaceChannel.value,
                                spaceChannelIconUrl: selectedSpaceChannel.iconUrl,
                                slackTeamId: null,
                                slackChannelId: selectedSlackChannel.value,
                                isMemberInSlackChannel: true,
                                isAuthorizedInSpaceChannel: true,
                                userIsAdminInSpaceChannel: false,
                                isNewChannelForOptimisticUpdate: true,
                            };

                            setChannels([...channels, newChannel]);
                            syncChannels.startSync(setChannels);
                            setSelectedSlackChannel(null);
                            setSelectedSpaceChannel(null);
                        }}
                    />
                </div>
            </div>

            <SyncedChannels
                channels={channels}
                onRemoveChannel={(channelToRemove) => {
                    setChannels(channels.filter((channel) => channel.spaceChannelId !== channelToRemove.spaceChannelId));
                    syncChannels.removeChannelFromSync(channelToRemove, setChannels);
                }}
                reloadSyncedChannels={() => {
                    syncedChannels.refreshSyncedChannels(setChannels);
                }}
            />
        </div>
    )
}

function SyncedChannels(props) {
    return props.channels.map((syncedChannel) =>
        <div className="mainPageRow syncedChannelRow" key={syncedChannel.spaceChannelId}>
            <div className="mainPageCell">
                <div className="syncedChannel">
                    <ChannelIcon
                        optionType="Slack"
                        iconUrl={null}
                    />
                    <span>{syncedChannel.channelNameInSlack}</span>
                    <Spring/>
                    {
                        !syncedChannel.isMemberInSlackChannel &&
                        <Icon name="warning" style={{marginRight: '12px'}}/>
                    }
                </div>
                {
                    !syncedChannel.isMemberInSlackChannel &&
                    <div className="warningText">
                        <span>{"Add the app to the "}</span>
                        <span className="link"
                              onClick={() => redirectToSlackChannel(syncedChannel.slackChannelId)}>{"channel"}</span>
                        <span>{" in Slack"}</span>
                    </div>
                }
            </div>
            <div className="mainPageCell">
                <div className="syncedChannel">
                    <ChannelIcon
                        optionType="Space"
                        iconUrl={syncedChannel.spaceChannelIconUrl}
                    />
                    <span className="syncedChannelName">{syncedChannel.channelNameInSpace}</span>
                    <Spring/>
                    {
                        !syncedChannel.isAuthorizedInSpaceChannel &&
                        <Icon name="warning" style={{marginRight: '12px'}}/>
                    }
                </div>
                {
                    !syncedChannel.isAuthorizedInSpaceChannel &&
                    <Fragment>
                        {
                            syncedChannel.userIsAdminInSpaceChannel
                                ? <span className="warningText link"
                                        onClick={() => spacePermissions.approveChannelPermissions(syncedChannel.spaceChannelId, props.reloadSyncedChannels)}>{"Approve channel app permissions"}</span>
                                : <span className="warningText">{"Ask channel admin to approve permissions"}</span>

                        }
                    </Fragment>
                }
            </div>
            <div className="mainPageCell">
                <ChannelControls
                    synced={syncedChannel.isMemberInSlackChannel && syncedChannel.isAuthorizedInSpaceChannel && !syncedChannel.isNewChannelForOptimisticUpdate}
                    hasPermissionToStopSync={syncedChannel.userIsAdminInSpaceChannel}
                    onRemoveChannelFromSync={() => props.onRemoveChannel(syncedChannel)}
                />
            </div>
        </div>
    );
}
