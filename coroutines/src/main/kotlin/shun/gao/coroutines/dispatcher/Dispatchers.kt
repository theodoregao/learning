package shun.gao.coroutines.dispatcher

object Dispatchers {

    val Default by lazy {
        DispatcherContext(DefaultDispatcher)
    }

}
