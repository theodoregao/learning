package shun.gao.coroutines.core

import shun.gao.coroutines.CancellationException
import shun.gao.coroutines.Deferred
import shun.gao.coroutines.Job
import shun.gao.coroutines.cancel.suspendCancellableCoroutine
import shun.gao.coroutines.utils.parse
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

class DeferredCoroutine<T>(
    context: CoroutineContext
) : AbstractCoroutine<T>(context), Deferred<T> {
    override suspend fun await(): T {
        return when (val currentState = state.get()) {
            is CoroutineState.Cancelling,
            is CoroutineState.InComplete -> awaitSuspend()
            is CoroutineState.Complete<*> -> {
                coroutineContext[Job]?.isActive?.takeIf { !it }?.let {
                    throw CancellationException("Coroutine is cancelled.")
                }
                currentState.value.parse<T>() ?: throw currentState.exception!!
            }
        }
    }

    private suspend fun awaitSuspend() = suspendCancellableCoroutine<T> { continuation ->
        val disposable = doOnCompleted { result ->
            continuation.resumeWith(result)
        }
        continuation.invokeOnCancel { disposable.dispose() }
    }
}
