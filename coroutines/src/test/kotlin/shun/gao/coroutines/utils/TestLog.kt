package shun.gao.coroutines.utils

object TestLog {

    private const val isLogOn = false

    fun println(message: Any?) = if (isLogOn) {
        kotlin.io.println(message)
    } else Unit

}
