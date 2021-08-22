package shun.gao.coroutines.dispatcher

import kotlinx.coroutines.runBlocking
import shun.gao.coroutines.launch
import shun.gao.coroutines.scope.GlobalScope
import shun.gao.coroutines.utils.TestLog.println
import kotlin.test.Test
import kotlin.test.assertEquals

internal class DefaultDispatcherTest {

    @Test
    fun testDefaultDispatcher() {
        runBlocking {
            GlobalScope.launch(Dispatchers.Default) {
                println("launch on CoroutineScope: $this, thread: ${Thread.currentThread().name}")
                assertEquals("DefaultDispatcher-worker-0", Thread.currentThread().name)
            }
        }
    }

}
