package shun.gao.coroutines.core

import shun.gao.coroutines.Job
import shun.gao.coroutines.OnCancel

typealias OnCompleteT<T> = (Result<T>) -> Unit

interface Disposable {
    fun dispose()
}

class CompletionHandlerDisposable<T>(
    private val job: Job,
    val onComplete: OnCompleteT<T>
) : Disposable {
    override fun dispose() {
        job.remove(this)
    }
}

class CancellationHandlerDisposable(
    val job: Job,
    val onCancel: OnCancel
) : Disposable {
    override fun dispose() {
        job.remove(this)
    }
}

sealed class DisposableList {
    object Nil : DisposableList()
    class Cons(val head: Disposable, val tail: DisposableList) : DisposableList()

    fun remove(disposable: Disposable): DisposableList {
        return when (this) {
            Nil -> this
            is Cons -> {
                if (head == disposable) return tail
                else Cons(head, tail.remove(disposable))
            }
        }
    }

    inline fun <reified T : Disposable> loopOn(crossinline action: (T) -> Unit) = forEach {
        when (it) { is T -> action(it) }
    }
}

tailrec fun DisposableList.forEach(action: (Disposable) -> Unit): Unit = when (this) {
    DisposableList.Nil -> Unit
    is DisposableList.Cons -> {
        action(head)
        tail.forEach(action)
    }
}
