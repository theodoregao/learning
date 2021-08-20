package shun.gao.coroutines.simulate

import shun.gao.coroutines.utils.parse
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.*

sealed class Status {
    class Created(val continuation: Continuation<Unit>) : Status()
    class Yielded<P>(val continuation: Continuation<P>) : Status()
    class Resumed<R>(val continuation: Continuation<R>) : Status()
    object Dead : Status()
}

class AsymmetricCoroutine<P, R> private constructor(
    override val context: CoroutineContext = EmptyCoroutineContext,
    private val block : suspend AsymmetricCoroutine<P, R>.CoroutineBody.(P) -> R
) : Continuation<R> {

    companion object {
        fun <P, R> create(
            context: CoroutineContext = EmptyCoroutineContext,
            block: suspend AsymmetricCoroutine<P, R>.CoroutineBody.(P) -> R
        ) : AsymmetricCoroutine<P, R> {
            return AsymmetricCoroutine(context, block)
        }
    }

    inner class CoroutineBody {
        var parameter: P? = null

        suspend fun yield(value: R): P = suspendCoroutine { continuation ->
            val previousStatus = status.getAndUpdate {
                when (it) {
                    is Status.Created -> throw IllegalStateException("Never started!")
                    is Status.Yielded<*> -> throw IllegalStateException("Already yielded!")
                    is Status.Resumed<*> -> Status.Yielded(continuation)
                    Status.Dead -> throw IllegalStateException("Already dead!")
                }
            }
            previousStatus.parse<Status.Resumed<R>>()?.continuation?.resume(value)
        }
    }

    private val body = CoroutineBody()

    private val status: AtomicReference<Status>

    val isActive: Boolean
        get() = status.get() != Status.Dead

    init {
        val coroutineBlock: suspend CoroutineBody.() -> R = { block(parameter!!) }
        val start = coroutineBlock.createCoroutine(body, this)
        status = AtomicReference(Status.Created(start))
    }

    override fun resumeWith(result: Result<R>) {
        val previousStatus = status.getAndUpdate {
            when (it) {
                is Status.Created -> throw IllegalStateException("Never started!")
                is Status.Yielded<*> -> throw IllegalStateException("Already yielded!")
                is Status.Resumed<*> -> Status.Dead
                Status.Dead -> throw IllegalStateException("Already dead!")
            }
        }
        previousStatus.parse<Status.Resumed<R>>()?.continuation?.resumeWith(result)
    }

    suspend fun resume(value: P): R = suspendCoroutine { continuation ->
        val previousStatus = status.getAndUpdate {
            when (it) {
                is Status.Created -> {
                    body.parameter = value
                    Status.Resumed(continuation)
                }
                is Status.Yielded<*> -> Status.Resumed(continuation)
                is Status.Resumed<*> -> throw IllegalStateException("Already resumed!")
                Status.Dead -> throw IllegalStateException("Already dead!")
            }
        }
        when (previousStatus) {
            is Status.Created -> previousStatus.continuation.resume(Unit)
            is Status.Yielded<*> -> previousStatus.parse<Status.Yielded<P>>()?.continuation?.resume(value)
            else -> {} // do nothing
        }
    }
}
