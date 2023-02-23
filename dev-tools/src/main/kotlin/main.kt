import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.spaceSlackSync.SlackCredentials
import org.jetbrains.spaceSlackSync.db
import org.jetbrains.spaceSlackSync.slack.slackAppClient
import space.jetbrains.api.runtime.ktorClientForSpace
import java.io.File
import java.io.FileInputStream
import java.net.URL
import java.util.*

/**
 * Dev utility to retrieve data from Slack/Space to investigate problems
 */
fun main() {
    val slackMessage: SlackMessage = readSlackMessageLink() ?: return
    setEnvVarsFromLocalProperties()

    // initialize db connection and Slack credentials from config
    db.apply { }
    SlackCredentials

    val ktorClient = ktorClientForSpace()

    runBlocking {
        val slackTeam = db.slackTeams.getByDomain(slackMessage.slackOrgDomain)
        if (slackTeam == null) {
            println("Could not find Slack team")
            return@runBlocking
        }
        val slackClient = slackAppClient(slackTeam)
        val message = when (slackMessage) {
            is SlackMessage.RootMessage -> {
                slackClient.getMessage(slackMessage.channelId, slackMessage.messageId)
            }

            is SlackMessage.ThreadMessage -> {
                slackClient.getThreadMessage(
                    slackMessage.channelId,
                    slackMessage.rootMessageId,
                    slackMessage.threadMessageId
                )
            }
        }

        println(message?.attachments?.size)
    }
}

private sealed class SlackMessage(
    val slackOrgDomain: String,
    val channelId: String,
) {
    class RootMessage(slackOrgDomain: String, channelId: String, val messageId: String) :
        SlackMessage(slackOrgDomain, channelId)

    class ThreadMessage(
        slackOrgDomain: String,
        channelId: String,
        val rootMessageId: String,
        val threadMessageId: String
    ) :
        SlackMessage(slackOrgDomain, channelId)
}

private fun setEnvVarsFromLocalProperties() {
    val file = File("./dev-tools/local.properties")
    val prop = Properties()
    FileInputStream(file).use { prop.load(it) }

    prop.stringPropertyNames()
        .associateWith { prop.getProperty(it) }
        .forEach {
            System.setProperty(it.key, it.value)
        }
}

private fun readSlackMessageLink(): SlackMessage? {
    print("Link to message: ")
    val url = readln()
    if (url.isEmpty()) {
        println("Link cannot be empty")
        return null
    }

    val slackOrgDomain = slackWorkspaceDomain(url) ?: return null
    val channelId = channelId(url) ?: return null
    val rootMessageId = getIdOfRootMessage(url) ?: return null
    return if ("thread_ts=" in url) {
        // https://jetbrains.slack.com/archives/C0X9M5TAS/p1675092419852709?thread_ts=1675087427.381789&cid=C0X9M5TAS
        val threadMessageId = url.substringAfter("thread_ts=").substringBefore("&")
        SlackMessage.ThreadMessage(slackOrgDomain, channelId, rootMessageId, threadMessageId)
    } else {
        SlackMessage.RootMessage(slackOrgDomain, channelId, rootMessageId)
    }
}

private fun channelId(url: String): String? {
    val channelId = url.substringBeforeLast("/").substringAfterLast("/")
    if (channelId.isEmpty()) {
        println("Couldn't extract slackChannelId from the URL")
        return null
    }
    return channelId
}

private fun slackWorkspaceDomain(url: String): String? {
    val domain = URL(url).host.substringBefore(".")
    if (domain.isEmpty()) {
        println("Couldn't extract slackOrgDomain from the URL")
        return null
    }
    return domain
}

private fun getIdOfRootMessage(url: String): String? {
    val slackMessageIdFromLink = url.substringAfterLast("/")
    if (slackMessageIdFromLink.isEmpty()) {
        println("Couldn't extract slackMessageId from the URL")
        return null
    }

    val withoutP = slackMessageIdFromLink.substringAfter("p").substringBefore("?")
    val lastSix = withoutP.takeLast(6)
    val beforeLastSix = withoutP.take(withoutP.length - 6)
    return "$beforeLastSix.$lastSix"
}

@Suppress("unused")
private fun String.prettyPrintedJson(): String {
    val mapper = ObjectMapper()
    val obj = mapper.readTree(this)
    return mapper.writerWithDefaultPrettyPrinter()
        .writeValueAsString(obj)
}
