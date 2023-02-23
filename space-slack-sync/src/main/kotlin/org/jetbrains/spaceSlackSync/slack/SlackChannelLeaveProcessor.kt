package org.jetbrains.spaceSlackSync.slack

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

suspend fun MessageFromSlackCtx.processChannelLeave(event: SlackMessageEvent.ChannelLeave) {
    log.debug("SKIP Slack message: channel leave is not processed for now")
    // for now skip channel leave message
}

private val log: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
