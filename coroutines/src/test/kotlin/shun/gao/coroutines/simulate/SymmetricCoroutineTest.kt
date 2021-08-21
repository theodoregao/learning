package shun.gao.coroutines.simulate

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

import shun.gao.coroutines.utils.TestLog.println

internal class SymmetricCoroutineTest {

    object Workers {
        val workerA: SymmetricCoroutine<Int> = SymmetricCoroutine.create { param ->
            println("worker A input $param,\n  transfer B")
            assertEquals(0, param)
            var result = transfer(workerB, param + 1)
            println("worker A result from B: $result,\n  transfer D")
            assertEquals(3, result)
            result = transfer(workerD, result + 1)
            println("worker A result from D: $result,\n  return")
            fail("Not suppose to reach here")

        }

        val workerB: SymmetricCoroutine<Int> = SymmetricCoroutine.create { param ->
            println("worker B input $param,\n  transfer C")
            assertEquals(1, param)
            var result = transfer(workerC, param + 1)
            println("worker B result from C: $result, return")
            fail("Not suppose to reach here")
        }

        val workerC: SymmetricCoroutine<Int> = SymmetricCoroutine.create { param ->
            println("worker C input $param,\n  transfer A")
            assertEquals(2, param)
            var result = transfer(workerA, param + 1)
            println("worker C result from A: $result,\n  transfer main")
            assertEquals(5, result)
            result = transfer(SymmetricCoroutine.main, result + 1)
            println("worker C result from main: $result,\n  return")
            fail("Not suppose to reach here")
        }

        val workerD: SymmetricCoroutine<Int> = SymmetricCoroutine.create { param ->
            println("worker D input $param,\n  transfer C")
            assertEquals(4, param)
            var result = transfer(workerC, param + 1)
            println("worker D result from C: $result,\n  return")
            fail("Not suppose to reach here")
        }
    }

    @Test
    fun `test main-A-B-C-A-D-C-main flow`() {
        runBlocking {
            SymmetricCoroutine.main {
                println("main transfer A")
                val result = transfer(Workers.workerA, 0)
                println("main end result from A: $result,\n  return")
                assertEquals(6, result)
            }
        }
    }
}
