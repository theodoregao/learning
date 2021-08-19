package shun.gao.coroutines.simulate

import kotlin.test.Test
import kotlin.test.assertEquals

internal class GeneratorKtTest {

    @Test
    fun testGenerator() {
        val numbers = generator { start: Int ->
            for (i in 0..5) {
                yield(start + i)
            }
        }
        var n = 10
        val seq = numbers(n)
        for (x in seq) {
            assertEquals(n++, x)
        }
    }

    @Test
    fun testFibonacciSequence() {
        val fibonacci = generator { from: Int ->
            var a = 1
            var b = 1
            var c: Int
            var n = 0
            while (true) {
                if (from <= n) {
                    yield(a)
                }
                c = a + b
                a = b
                b = c
                n++
            }
        }

        val fibonacciSeq = fibonacci(0).iterator()
        val expectedFibonacciSeqSeq = listOf(1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89)
        for (i in 0..10) {
            assertEquals(expectedFibonacciSeqSeq[i], fibonacciSeq.next())
        }
    }
}
