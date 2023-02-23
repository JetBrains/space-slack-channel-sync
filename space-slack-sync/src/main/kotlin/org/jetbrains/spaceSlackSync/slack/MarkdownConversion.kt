package org.jetbrains.spaceSlackSync.slack

fun convertMarkdown(slackText: String): String {
    return buildString {
        var i = 0
        while (i < slackText.length) {
            val linkStartIndex = slackText.indexOf('<', i)
            if (linkStartIndex == -1) {
                append(slackText.substring(i))
                break
            }
            append(slackText.substring(i, linkStartIndex))

            val linkEndIndex = slackText.indexOf('>', linkStartIndex)
            if (linkEndIndex == -1) {
                append(slackText.substring(i))
                break
            }

            val link = slackText.substring(linkStartIndex + 1, linkEndIndex)
            val delimiterIndex = link.lastIndexOf('|')
            if (delimiterIndex != -1) {
                val url = link.substring(0, delimiterIndex)
                val name = link.substring(delimiterIndex + 1)
                append("[$name]($url)")
            } else {
                append("<$link>")
            }

            i = linkEndIndex + 1
        }
    }
}
