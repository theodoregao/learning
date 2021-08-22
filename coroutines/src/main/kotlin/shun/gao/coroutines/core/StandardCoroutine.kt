package shun.gao.coroutines.core

import shun.gao.coroutines.excpetion.CoroutineExceptionHandler
import kotlin.coroutines.CoroutineContext

class StandardCoroutine(
    context: CoroutineContext
) : AbstractCoroutine<Unit>(context) {
    override fun handleJobException(e: Throwable): Boolean {
        context[CoroutineExceptionHandler]
            ?.handleException(context, e)
            ?: Thread.currentThread().let {
                it.uncaughtExceptionHandler.uncaughtException(it, e)
            }
        return true
    }
}
