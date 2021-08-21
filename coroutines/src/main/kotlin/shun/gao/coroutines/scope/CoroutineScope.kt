package shun.gao.coroutines.scope

import kotlin.coroutines.CoroutineContext

interface CoroutineScope {
    val scopeContext: CoroutineContext
}
