package shun.gao.coroutines.simulate

import kotlin.coroutines.*

interface AsyncScope

suspend fun <T> AsyncScope.await(
    block: () -> T
) = suspendCoroutine<T> { continuation ->
    try {
        val result = block()
        continuation.resume(result)
    } catch (e: Throwable) {
        continuation.resumeWithException(e)
    }
}

class AsyncCoroutine(
    override val context: CoroutineContext = EmptyCoroutineContext
) : Continuation<Unit>, AsyncScope {
    override fun resumeWith(result: Result<Unit>) {
        result.getOrThrow()
    }
}

fun async(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend AsyncScope.() -> Unit
) {
    val companion = AsyncCoroutine(context)
    block.startCoroutine(companion, companion)
}
