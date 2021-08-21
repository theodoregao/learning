package shun.gao.coroutines.cancel

sealed class CancelState {
    override fun toString(): String {
        return "CancelState.${javaClass.simpleName}"
    }

    object InComplete : CancelState()
    class Complete<T>(val value: T? = null, val exception: Throwable? = null) : CancelState()
    object Cancelled : CancelState()
}
