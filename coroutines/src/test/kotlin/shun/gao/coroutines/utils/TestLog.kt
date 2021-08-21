package shun.gao.coroutines.utils

object TestLog {

    const val isLogOn = false

    inline fun println(message: Any?) = if (isLogOn) {
        kotlin.io.println(message)
    } else Unit

}
