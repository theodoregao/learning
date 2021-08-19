package shun.gao.coroutines.simulate

import shun.gao.coroutines.utils.parse
import kotlin.coroutines.*

interface Generator<T> {
    operator fun iterator() : Iterator<T>
}

fun <T> generator(block: suspend GeneratorScope<T>.(T) -> Unit): (T) -> Generator<T> {
    return { parameter: T ->
        GeneratorImpl(block, parameter)
    }
}

sealed class State {
    class NotReady(val continuation: Continuation<Unit>) : State()
    class Ready<T>(val continuation: Continuation<Unit>, val nextValue: T) : State()
    object Done : State()
}

abstract class GeneratorScope<T> internal constructor() {
    protected abstract val parameter: T

    abstract suspend fun yield(value: T)
}

class GeneratorImpl<T>(
    private val block: suspend GeneratorScope<T>.(T) -> Unit,
    private val parameter: T
) : Generator<T> {
    override fun iterator() : Iterator<T> {
        return GeneratorIterator(block, parameter)
    }
}

class GeneratorIterator<T>(
    private val block: suspend GeneratorScope<T>.(T) -> Unit,
    override val parameter: T
): GeneratorScope<T>(), Iterator<T>, Continuation<Any?> {

    override val context: CoroutineContext = EmptyCoroutineContext

    private var state: State

    init {
        val coroutineBlock: suspend GeneratorScope<T>.() -> Unit = { block(parameter) }
        val start = coroutineBlock.createCoroutine(this, this)
        state = State.NotReady(start)
    }

    override suspend fun yield(value: T) = suspendCoroutine<Unit> { continuation ->
        state = when (state) {
            is State.NotReady -> State.Ready(continuation, value)
            is State.Ready<*> -> throw IllegalStateException("Cannot yield a value while ready.")
            State.Done -> throw IllegalStateException("Cannot yield a value while done.")
        }
    }

    override fun hasNext(): Boolean {
        resume()
        return state != State.Done
    }

    override fun next(): T {
        return when (val currentState = state) {
            is State.NotReady -> {
                resume()
                return next()
            }
            is State.Ready<*> -> {
                state = State.NotReady(currentState.continuation)
                currentState.parse<State.Ready<T>>()!!.nextValue
            }
            State.Done -> throw IndexOutOfBoundsException("No value left.")
        }
    }

    override fun resumeWith(result: Result<Any?>) {
        state = State.Done
        result.getOrThrow()
    }

    private fun resume() {
        when (val currentState = state) {
            is State.NotReady -> currentState.continuation.resume(Unit)
        }
    }
}
