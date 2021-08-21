package shun.gao.coroutines.core

import kotlinx.coroutines.CoroutineName
import shun.gao.coroutines.CancellationException
import shun.gao.coroutines.Job
import shun.gao.coroutines.OnCancel
import shun.gao.coroutines.OnComplete
import shun.gao.coroutines.cancel.suspendCancellableCoroutine
import shun.gao.coroutines.scope.CoroutineScope
import shun.gao.coroutines.utils.parse
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.*

abstract class AbstractCoroutine<T>(
    context: CoroutineContext
) : Job, Continuation<T>, CoroutineScope {

    protected val state = AtomicReference<CoroutineState>()

    override val context: CoroutineContext = context + this

    override val scopeContext: CoroutineContext get() = context

    protected val parentJob = context[Job]

    private var parentCancelDisposable: Disposable? = null

    init {
        state.set(CoroutineState.InComplete())
    }

    override val isActive: Boolean
        get() = state.get() is CoroutineState.InComplete

    override val isCompleted: Boolean
        get() = state.get() is CoroutineState.Complete<*>

    override fun invokeOnCompletion(onComplete: OnComplete): Disposable {
        return doOnCompleted {
            onComplete()
        }
    }

    override fun remove(disposable: Disposable) {
        state.updateAndGet { prev ->
            when (prev) {
                is CoroutineState.InComplete -> CoroutineState.InComplete().from(prev).without(disposable)
                is CoroutineState.Cancelling -> CoroutineState.Cancelling().from(prev).without(disposable)
                is CoroutineState.Complete<*> -> prev
            }
        }
    }

    override suspend fun join() {
        when (state.get()) {
            is CoroutineState.InComplete,
            is CoroutineState.Cancelling -> return joinSuspend()
            is CoroutineState.Complete<*> -> {
                val currentCancellingJobState = coroutineContext[Job]?.isActive ?: return
                if (!currentCancellingJobState) {
                    throw CancellationException("Coroutine is cancelled.")
                }
            }
        }
    }

    private suspend fun joinSuspend() = suspendCancellableCoroutine<Unit> { continuation ->
        val disposable = doOnCompleted { continuation.resume((Unit)) }
        continuation.invokeOnCancel { disposable.dispose() }
    }

    protected fun doOnCompleted(block: (Result<T>) -> Unit): Disposable {
        val disposable = CompletionHandlerDisposable(this, block)
        val newState = state.updateAndGet { prev ->
            when (prev) {
                is CoroutineState.InComplete -> CoroutineState.InComplete().from(prev).with(disposable)
                is CoroutineState.Cancelling -> CoroutineState.Cancelling().from(prev).with(disposable)
                is CoroutineState.Complete<*> -> prev
            }
        }

        newState.parse<CoroutineState.Complete<T>>()?.let {
            block(when {
                it.value != null -> Result.success(it.value)
                it.exception != null -> Result.failure(it.exception)
                else -> throw IllegalStateException("Should not happen.")
            })
        }

        return disposable
    }

    override fun resumeWith(result: Result<T>) {
        val newState = state.updateAndGet { prev ->
            when (prev) {
                is CoroutineState.Cancelling,
                is CoroutineState.InComplete ->
                    CoroutineState.Complete(result.getOrNull(), result.exceptionOrNull()).from(prev)
                is CoroutineState.Complete<*> -> throw IllegalStateException("Already completed.")
            }
        }
        newState.parse<CoroutineState.Complete<*>>()?.exception?.let(::tryHandleException)
        newState.notifyCompletion(result)
        newState.clear()
        parentCancelDisposable?.dispose()
    }

    private fun tryHandleException(e: Throwable): Boolean {
        return when (e) {
            is CancellationException -> false
            else -> parentJob.parse<AbstractCoroutine<*>>()
                ?.handleChildException(e)
                ?.takeIf { it }
                ?: handleJobException(e)
        }
    }

    protected open fun handleJobException(e: Throwable) = false

    protected open fun handleChildException(e: Throwable): Boolean {
        cancel()
        return tryHandleException(e)
    }

    override fun invokeOnCancel(onCancel: OnCancel): Disposable {
        val disposable = CancellationHandlerDisposable(this, onCancel)
        val newState = state.updateAndGet { prev ->
            when (prev) {
                is CoroutineState.InComplete -> CoroutineState.InComplete().from(prev).with(disposable)
                is CoroutineState.Cancelling,
                is CoroutineState.Complete<*> -> prev
            }
        }

        newState.parse<CoroutineState.Cancelling>()?.let { onCancel() }
        return disposable
    }

    override fun cancel() {
        val newState = state.updateAndGet { prev ->
            when (prev) {
                is CoroutineState.InComplete -> CoroutineState.Cancelling().from(prev)
                is CoroutineState.Cancelling,
                is CoroutineState.Complete<*> -> prev
            }
        }

        if (newState is CoroutineState.Cancelling) {
            newState.notifyCancellation()
        }

        parentCancelDisposable?.dispose()
    }

    override fun toString(): String {
        return "${context[CoroutineName]?.name}"
    }

}
