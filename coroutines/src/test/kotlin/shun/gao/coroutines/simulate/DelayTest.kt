package shun.gao.coroutines.javaimpl

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

internal class AsymmetricCoroutineTest {

    @Test
    fun testDelay() {
        runBlocking {
            val currentTimeStamp = System.currentTimeMillis()
            delay(1000)
            assertTrue(System.currentTimeMillis() >= currentTimeStamp + 1000)
            assertTrue(System.currentTimeMillis() < currentTimeStamp + 1300)
            delay(2000)
            assertTrue(System.currentTimeMillis() >= currentTimeStamp + 3000)
            assertTrue(System.currentTimeMillis() < currentTimeStamp + 3300)
            delay(3000)
            assertTrue(System.currentTimeMillis() >= currentTimeStamp + 6000)
            assertTrue(System.currentTimeMillis() < currentTimeStamp + 6300)
        }
    }
}
