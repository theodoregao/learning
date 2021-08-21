package shun.gao.coroutines.cancel

import shun.gao.coroutines.CancellationException
import shun.gao.coroutines.Job
import shun.gao.coroutines.OnCancel
import shun.gao.coroutines.utils.parse
import java.lang.IllegalStateException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.intercepted
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resumeWithException

class CancellationContinuation<T>(
    private val continuation: Continuation<T>
) : Continuation<T> by continuation {
    private val state = AtomicReference<CancelState>(CancelState.InComplete)

    private val cancelHandlers = CopyOnWriteArrayList<OnCancel>()

    val isCompleted: Boolean get() = state.get() is CancelState.Complete<*>

    val isActive: Boolean get() = state.get() == CancelState.InComplete

    override fun resumeWith(result: Result<T>) {
        state.updateAndGet { prev ->
            when (prev) {
                CancelState.InComplete -> {
                    continuation.resumeWith(result)
                    CancelState.Complete(result.getOrNull(), result.exceptionOrNull())
                }
                is CancelState.Complete<*> -> throw IllegalStateException("Already completed.")
                CancelState.Cancelled -> {
                    CancellationException("Cancelled.").let {
                        continuation.resumeWith(Result.failure(it))
                        CancelState.Complete(null, it)
                    }
                }
            }
        }
    }

    fun cancel() {
        if (isActive) return
        val parent = continuation.context[Job] ?: return
        parent.cancel()
    }

    fun invokeOnCancel(onCancel: OnCancel) {
        cancelHandlers += onCancel
    }

    fun getResult(): Any? {
        installCancelHandler()
        return when (val currentState = state.get()) {
            CancelState.InComplete -> COROUTINE_SUSPENDED
            is CancelState.Complete<*> ->
                currentState.parse<CancelState.Complete<T>>()
                    ?.let { it.exception?.let { throw it } ?: it.value }
            CancelState.Cancelled -> throw CancellationException("Continuation is cancelled.")
        }
    }

    private fun installCancelHandler() {
        if (!isActive) return
        val parent = continuation.context[Job] ?: return
        parent.invokeOnCancel { doCancel() }
    }

    private fun doCancel() {
        val prevState = state.getAndUpdate { prev ->
            when (prev) {
                CancelState.InComplete -> CancelState.Cancelled
                is CancelState.Complete<*>, CancelState.Cancelled -> prev
            }
        }

        if (prevState is CancelState.Cancelled) {
            cancelHandlers.forEach(OnCancel::invoke)
            cancelHandlers.clear()
            resumeWithException(CancellationException("Cancelled."))
        }
    }
}

suspend inline fun <T> suspendCancellableCoroutine(
    crossinline block: (CancellationContinuation<T>) -> Unit
) : T = suspendCoroutineUninterceptedOrReturn { continuation ->
    val cancellationContinuation = CancellationContinuation(continuation.intercepted())
    block(cancellationContinuation)
    cancellationContinuation.getResult()
}
