package org.jetbrains.spaceSlackSync.slack

suspend fun MessageFromSlackCtx.processChannelLeave(event: SlackMessageEvent.ChannelLeave) {
    // for now skip channel leave message
}
