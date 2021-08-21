package shun.gao.coroutines.simulate

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

import shun.gao.coroutines.common.DispatcherContext
import shun.gao.coroutines.utils.TestLog.println

internal class AsymmetricCoroutineTest {

    @Test
    fun testProducerCustomer() {
        val producer = AsymmetricCoroutine.create<Unit, Int>(DispatcherContext()) {
            for (i in 0..100) {
                println("produced in ${Thread.currentThread().name}")
                yield(i) // produce the product
            }
            200 // this value never been used
        }

        val consumer = AsymmetricCoroutine.create<Int, Unit>(DispatcherContext()) { firstProduct ->
            consumeProduct(0, firstProduct)
            for (i in 1..100) {
                println("consumed in ${Thread.currentThread().name}")
                val product = yield(Unit)
                consumeProduct(i, product)
            }
        }

        runBlocking {
            while (producer.isActive && consumer.isActive) {
                val result = producer.resume(Unit)
                consumer.resume(result)
            }
        }
    }

    private fun consumeProduct(expectedProduct: Int, product: Int) {
        assertEquals(expectedProduct, product)
    }
}
