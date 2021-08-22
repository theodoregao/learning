package shun.gao.coroutines.utils

object TestLog {

    private const val isLogOn = true

    fun println(message: Any?) = if (isLogOn) {
        kotlin.io.println(message)
    } else Unit

}
