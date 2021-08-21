package shun.gao.coroutines.simulate

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class SymmetricCoroutine<T> private constructor(
    override val context: CoroutineContext = EmptyCoroutineContext,
    private val block: suspend SymmetricCoroutine<T>.SymmetricCoroutineBody.(T) -> Unit
) : Continuation<T> {

    companion object {
        lateinit var main : SymmetricCoroutine<Any?>

        suspend fun main(block: suspend SymmetricCoroutine<Any?>.SymmetricCoroutineBody.() -> Unit) {
            SymmetricCoroutine<Any?> { block() }.also { main = it }.start(Unit)
        }

        fun <T> create(
            context: CoroutineContext = EmptyCoroutineContext,
            block: suspend SymmetricCoroutine<T>.SymmetricCoroutineBody.(T) -> Unit
        ) : SymmetricCoroutine<T> {
            return SymmetricCoroutine(context, block)
        }
    }

    inner class SymmetricCoroutineBody {
        private tailrec suspend fun <P> transferInner(symmetricCoroutine: SymmetricCoroutine<P>, value: Any?): T {
            if (this@SymmetricCoroutine.isMain) {
                return if (symmetricCoroutine.isMain) {
                    value as T
                } else {
                    val parameter = symmetricCoroutine.coroutine.resume(value as P)
                    transferInner(parameter.coroutine, parameter.value)
                }
            } else {
                this@SymmetricCoroutine.coroutine.run {
                    return yield(Parameter(symmetricCoroutine, value as P))
                }
            }
        }

        suspend fun <P> transfer(symmetricCoroutine: SymmetricCoroutine<P>, value: P): T {
            return transferInner(symmetricCoroutine, value)
        }
    }

    class Parameter<T>(val coroutine: SymmetricCoroutine<T>, val value: T)

    val isMain: Boolean
        get() = this == main

    private val body = SymmetricCoroutineBody()

    private val coroutine = AsymmetricCoroutine.create<T, Parameter<*>>(context) {
        Parameter(this@SymmetricCoroutine, suspend {
            block(body, it)
            if (this@SymmetricCoroutine.isMain) Unit else throw IllegalStateException("SymmetricCoroutine cannot not be dead.")
        }() as T)
    }

    override fun resumeWith(result: Result<T>) {
        throw IllegalStateException("SymmetricCoroutine cannot be dead.")
    }

    suspend fun start(value: T) {
        coroutine.resume(value)
    }
}
