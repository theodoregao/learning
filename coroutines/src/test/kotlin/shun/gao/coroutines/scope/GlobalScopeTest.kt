package shun.gao.coroutines.scope

import kotlinx.coroutines.runBlocking
import shun.gao.coroutines.async
import shun.gao.coroutines.javaimpl.delay
import shun.gao.coroutines.launch
import shun.gao.coroutines.utils.TestLog.println
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class GlobalScopeTest {

    @Test
    fun testGlobalScope() {
        val job = GlobalScope.launch {
            println("launch a new job $this")
            delay(100)
            println("finished job $this")
        }
        assertTrue(job.isActive)

        GlobalScope.launch {
            println("launch another job $this")
            job.join()
            assertFalse(job.isActive)
            assertTrue(job.isCompleted)
            println("finished job $this")
        }

        Thread.sleep(1000) // ensure all the job is done
    }

    @Test
    fun testDeferred() {
        runBlocking {
            val timestamp = System.currentTimeMillis()
            println("timestamp: $timestamp")
            GlobalScope.launch {
                val deferred = async {
                    getTimestamp()
                }
                val result = deferred.await()
                assertTrue(result - timestamp < 100)
            }
        }
    }

    private suspend fun getTimestamp(): Long {
        val timestamp = System.currentTimeMillis()
        println("timestamp in getTimestamp(): $timestamp")
        delay(100)
        return timestamp
    }
}