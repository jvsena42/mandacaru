package com.github.jvsena42.mandacaru.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CoroutineExtensionsTest {

    @Test
    fun `returns success with the block result`() {
        val result = runSuspendCatching { 42 }

        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `wraps a thrown exception in a failure result`() {
        val boom = IllegalStateException("boom")

        val result = runSuspendCatching<Int> { throw boom }

        assertTrue(result.isFailure)
        assertSame(boom, result.exceptionOrNull())
    }

    @Test(expected = CancellationException::class)
    fun `rethrows CancellationException instead of wrapping it`() {
        runSuspendCatching { throw CancellationException("cancelled") }
    }

    @Test
    fun `supports suspend calls inside the block`() = runBlocking {
        suspend fun echo(value: Int): Int {
            delay(1)
            return value
        }

        val result = runSuspendCatching { echo(7) }

        assertEquals(7, result.getOrNull())
    }

    @Test
    fun `does not swallow coroutine cancellation so the job actually cancels`() = runBlocking {
        var completedNormally = false
        var capturedResult: Result<Unit>? = null

        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            capturedResult = runSuspendCatching { delay(Long.MAX_VALUE) }
            completedNormally = true
        }

        job.cancelAndJoin()

        assertTrue(job.isCancelled)
        assertFalse("runSuspendCatching swallowed the cancellation", completedNormally)
        assertNull("cancellation must propagate, not produce a Result", capturedResult)
    }
}
