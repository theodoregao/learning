package shun.gao.coroutines

interface Deferred<T>: Job {
    suspend fun await(): T
}
