package shun.gao.coroutines.javaimpl

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private val executor = Executors.newScheduledThreadPool(1) { runnable ->
    Thread(runnable, "Delay-Scheduler").apply { isDaemon = true}
}

suspend fun delay(time: Long, unit: TimeUnit = TimeUnit.MILLISECONDS) = suspendCoroutine<Unit> {
    executor.schedule({ it.resume(Unit) }, time, unit)
}
