package com.natural.androidunittestsandbox

import kotlinx.coroutines.*
import org.junit.Test

import org.junit.Assert.*
import java.util.concurrent.atomic.AtomicLong

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun `Launch must in coroutine scope`() {
        GlobalScope.launch {
            delay(1000)
            println("Hello")
        }

        println("Start")

        runBlocking {
            delay(1000)
        }

        println("Stop")
    }

    @Test
    fun `Coroutines are cheaper`() {
        val c = AtomicLong()

        runBlocking {

            for (i in 1..1_000_000L) {
                GlobalScope.launch {
                    c.addAndGet(i)
                }
            }
        }

        println(c.get())

    }

    @Test
    fun `Async returning a value from a coroutine`() {
        val deferred = (1..1_000_000).map { n ->
            GlobalScope.async {
                n
            }
        }

        runBlocking {
            val sum = deferred.sumOf {
                it.await().toLong()
            }
            println(sum)
        }
    }

    @Test
    fun `Suspending functions`() {
        runBlocking {
            var t = workload(1)
            t = workload(t)
            t = workload(t)
            println(t)
        }
    }

    suspend fun workload(n: Int): Int {
        delay(1000)
        return n + 1
    }
}