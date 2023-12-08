import './mainPage.css';
import LogoWithNameAndHost from "./logoWithNameAndHost";
import * as spaceOrg from "../service/spaceOrg";
import * as spaceAuth from "../service/spaceAuth";
import React, {useState} from "react";
import * as slackChannels from "../service/slackChannels";
import * as spaceChannels from "../service/spaceChannels";
import ChannelSelectField, {ChannelSelectOption} from "./select";
import * as syncChannels from "../service/syncChannels";
import Button from "./button";
import * as syncedChannels from "../service/syncedChannels";
import {SyncedChannelInfo} from "../service/syncedChannels";
import ChannelIcon from "./channelIcon";
import Spring from "./spring";
import Icon from "./icon";
import ChannelControls from "./channelControls";
import {redirectToSlackChannel} from "../service/utils";
import * as spacePermissions from "../service/spacePermissions";
import UnapprovedPermissionsWarning from "./unapprovedPermissionsWarning";
import {SlackWorkspace} from '../service/slackTeams';

interface MainPageProps {
    slackWorkspace: SlackWorkspace;
}

export default function MainPage(props: MainPageProps) {
    let [readyToSync, setReadyToSync] = useState<boolean>(syncChannels.readyToSync());
    let [channels, setChannels] = useState<SyncedChannelInfo[]>(syncedChannels.getSyncedChannels());
    let [selectedSlackChannel, setSelectedSlackChannel] = useState<ChannelSelectOption | null>(null);
    let [selectedSpaceChannel, setSelectedSpaceChannel] = useState<ChannelSelectOption | null>(null);

    return <>
        <div className="mainPage">
            <UnapprovedPermissionsWarning/>
            <table className="mainPageTable">
                <colgroup>
                    <col span={1} style={{width: '45%'}}></col>
                    <col span={1} style={{width: '45%'}}></col>
                    <col span={1} style={{width: '10%'}}></col>
                </colgroup>

                <tbody>
                <tr>
                    <td className="mainPageTd">
                        <LogoWithNameAndHost
                            renderLogo={() => (<Icon name="slack"/>)}
                            name={props.slackWorkspace.name}
                            host={`${props.slackWorkspace.domain}.slack.com`}
                        />
                    </td>
                    <td className="mainPageTd">
                        <LogoWithNameAndHost
                            renderLogo={() => (<Icon name="space"/>)}
                            name={spaceOrg.getOrgName()}
                            host={spaceAuth.getSpaceDomain()}
                        />
                    </td>
                    <td className="mainPageTd">
                    </td>
                </tr>
                <tr>
                    <td className="mainPageTd">
                        <div className="pickChannelSpan">{"Pick a Slack channel"}</div>
                    </td>
                    <td className="mainPageTd">
                        <div className="pickChannelSpan">{"Pick a Space channel"}</div>
                    </td>
                    <td className="mainPageTd">
                    </td>
                </tr>
                <tr>
                    <td className="mainPageTd selectRowTd">
                        <ChannelSelectField
                            defaultOptions={slackChannels.getDefaultChannelsAsSelectOptions()}
                            loadOptions={slackChannels.loadChannelsAsSelectOptions}
                            onChange={(selectedOption) => {
                                setSelectedSlackChannel(selectedOption);
                                if (selectedOption != null) {
                                    syncChannels.setSlackChannelToSync(selectedOption.value);
                                }
                                setReadyToSync(syncChannels.readyToSync());
                            }}
                            value={selectedSlackChannel}
                        />
                    </td>
                    <td className="mainPageTd selectRowTd">
                        <ChannelSelectField
                            defaultOptions={spaceChannels.getDefaultChannelsAsSelectOptions()}
                            loadOptions={spaceChannels.loadChannelsAsSelectOptions}
                            onChange={(selectedOption) => {
                                setSelectedSpaceChannel(selectedOption);
                                if (selectedOption != null) {
                                    syncChannels.setSpaceChannelToSync(selectedOption.value);
                                }
                                setReadyToSync(syncChannels.readyToSync());
                            }}
                            value={selectedSpaceChannel}
                        />
                    </td>
                    <td className="mainPageTd selectRowTd">
                        <Button
                            buttonText="Create tunnel"
                            isDisabled={!readyToSync}
                            actionHandler={() => {
                                if (selectedSpaceChannel !== null && selectedSlackChannel !== null) {
                                    const newChannel: SyncedChannelInfo = {
                                        // optimistic update
                                        channelNameInSpace: selectedSpaceChannel.label,
                                        channelNameInSlack: selectedSlackChannel.label,
                                        spaceChannelId: selectedSpaceChannel.value,
                                        spaceChannelIconUrl: selectedSpaceChannel.icon,
                                        slackTeamId: props.slackWorkspace.id,
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
                                }
                            }}
                        />
                    </td>
                </tr>
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
                </tbody>
            </table>
        </div>
    </>
}

interface SyncedChannelsProps {
    channels: SyncedChannelInfo[];
    reloadSyncedChannels: () => void;
    onRemoveChannel: (channel: SyncedChannelInfo) => void;
}

function SyncedChannels(props: SyncedChannelsProps) {
    return <>
        {
            props.channels.map((syncedChannel) =>
                <>
                    <tr key={syncedChannel.spaceChannelId}>
                        <td className="mainPageTd ">
                            <div className="syncedChannel">
                                <ChannelIcon
                                    optionType="Slack"
                                    iconUrl={null}
                                />
                                <span className="syncedChannelName">{syncedChannel.channelNameInSlack}</span>
                                <Spring/>
                                {
                                    !syncedChannel.isMemberInSlackChannel &&
                                    <Icon name="warning" style={{marginRight: '12px'}}/>
                                }
                            </div>
                        </td>
                        <td className="mainPageTd">
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
                        </td>
                        <td className="mainPageTd">
                            <ChannelControls
                                synced={syncedChannel.isMemberInSlackChannel && syncedChannel.isAuthorizedInSpaceChannel && !syncedChannel.isNewChannelForOptimisticUpdate}
                                hasPermissionToStopSync={true}
                                onRemoveChannelFromSync={() => props.onRemoveChannel(syncedChannel)}
                            />
                        </td>
                    </tr>
                    <tr>
                        <td className="channelWarningRowTd">
                            {
                                !syncedChannel.isMemberInSlackChannel &&
                                <div className="warningText">
                                    <span>{"Add the app to the "}</span>
                                    <span className="link"
                                          onClick={() => redirectToSlackChannel(syncedChannel.slackChannelId)}>{"channel"}</span>
                                    <span>{" in Slack"}</span>
                                </div>
                            }
                        </td>
                        <td className="channelWarningRowTd">
                            {
                                !syncedChannel.isAuthorizedInSpaceChannel &&
                                <>
                                    {
                                        syncedChannel.userIsAdminInSpaceChannel
                                            ? <span className="warningText link"
                                                    onClick={() => spacePermissions.approveChannelPermissions(syncedChannel.spaceChannelId, props.reloadSyncedChannels)}>{"Approve channel app permissions"}</span>
                                            : <span
                                                className="warningText">{"Ask channel admin to approve permissions"}</span>

                                    }
                                </>
                            }
                        </td>
                        <td className="channelWarningRowTd">
                        </td>
                    </tr>
                </>
            )
        }
    </>
}
