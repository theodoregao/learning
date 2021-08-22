package shun.gao.coroutines.simulate

import shun.gao.coroutines.dispatcher.Dispatcher
import shun.gao.coroutines.dispatcher.DispatcherContext
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class AsyncAwaitTest {

    @Test
    fun testEmptyDispatcherWithAsyncAwait() {
        val currentTimeStamp = System.currentTimeMillis()
        async {
            val result = await { System.currentTimeMillis() }
            assertTrue { result - currentTimeStamp < 1000 }
        }
    }

    @Test
    fun testDefaultDispatchWithAsyncAwait() {
        val defaultDispatcher = DispatcherContext()
        val currentTimeStamp = System.currentTimeMillis()
        async(defaultDispatcher) {
            val result = await { System.currentTimeMillis() }
            assertTrue { result - currentTimeStamp < 1000 }
        }
    }

    @Test
    fun testCustomDispatchWithAsyncAwait() {
        val dispatcher = DispatcherContext(object : Dispatcher {
            override fun dispatch(block: () -> Unit) {
                Thread.sleep(3000)
                block()
            }
        })
        val currentTimeStamp = System.currentTimeMillis()
        async(dispatcher) {
            val result = await { System.currentTimeMillis() }
            assertTrue { result - currentTimeStamp > 3000 }
            assertTrue { result - currentTimeStamp < 4000 }
        }
    }

    @Test
    fun testCustomDispatchWithAsyncAwaitThrowException() {
        val dispatcher = DispatcherContext(object : Dispatcher {
            override fun dispatch(block: () -> Unit) {
                Thread.sleep(3000)
                throw RuntimeException("something went wrong")
            }
        })
        assertFailsWith(RuntimeException::class) {
            async(dispatcher) {
                await { System.currentTimeMillis() }
            }
        }
    }
}
