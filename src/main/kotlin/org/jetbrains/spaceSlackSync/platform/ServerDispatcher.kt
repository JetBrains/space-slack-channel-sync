package org.jetbrains.spaceSlackSync.platform

import kotlinx.coroutines.*
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

val serverDispatchThreadCount = (Runtime.getRuntime().availableProcessors() - 1).coerceAtLeast(2).coerceAtMost(4)

private val serverThreadFactory = object : ThreadFactory {
    private val index = AtomicInteger()

    override fun newThread(r: Runnable): Thread {
        return Thread(r, "event-processing-${index.incrementAndGet()}").apply { isDaemon = true }
    }
}

// TODO: make sure coroutines finish their execution when application node is stopped
private val Server = Executors.newScheduledThreadPool(serverDispatchThreadCount, serverThreadFactory)
    .asCoroutineDispatcher()

@OptIn(DelicateCoroutinesApi::class)
fun launch(block: suspend CoroutineScope.() -> Unit) = GlobalScope.launch(
    context = Server,
    block = block
)
