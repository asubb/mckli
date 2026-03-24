package com.mckli.transport

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class ReconnectionStrategyTest {

    @Test
    fun `initial delay is 1 second`() {
        val strategy = ReconnectionStrategy()
        val delay = strategy.getNextDelay()

        assertEquals(1000, delay)
    }

    @Test
    fun `exponential backoff increases delay`() = runTest {
        val strategy = ReconnectionStrategy(
            initialDelayMs = 1000,
            backoffMultiplier = 2.0
        )

        assertEquals(1000, strategy.getNextDelay()) // 1 * 2^0 = 1
        strategy.recordAttempt()

        assertEquals(2000, strategy.getNextDelay()) // 1 * 2^1 = 2
        strategy.recordAttempt()

        assertEquals(4000, strategy.getNextDelay()) // 1 * 2^2 = 4
        strategy.recordAttempt()

        assertEquals(8000, strategy.getNextDelay()) // 1 * 2^3 = 8
    }

    @Test
    fun `delay is capped at max delay`() = runTest {
        val strategy = ReconnectionStrategy(
            initialDelayMs = 1000,
            maxDelayMs = 5000,
            backoffMultiplier = 2.0
        )

        // After several attempts, delay should be capped
        repeat(5) { strategy.recordAttempt() }

        val delay = strategy.getNextDelay()
        assertTrue(delay!! <= 5000, "Delay should be capped at maxDelayMs")
    }

    @Test
    fun `max retries returns null`() = runTest {
        val strategy = ReconnectionStrategy(maxRetries = 3)

        // First 3 attempts should return delays
        assertTrue(strategy.recordAttempt() != null)
        assertTrue(strategy.recordAttempt() != null)
        assertTrue(strategy.recordAttempt() != null)

        // 4th attempt should return null (max retries exceeded)
        assertNull(strategy.getNextDelay())
    }

    @Test
    fun `reset clears attempt count`() = runTest {
        val strategy = ReconnectionStrategy()

        strategy.recordAttempt()
        strategy.recordAttempt()
        assertEquals(2, strategy.getAttemptCount())

        strategy.reset()
        assertEquals(0, strategy.getAttemptCount())
        assertEquals(1000, strategy.getNextDelay()) // Back to initial delay
    }

    @Test
    fun `isExhausted returns false initially`() {
        val strategy = ReconnectionStrategy(maxRetries = 3)
        assertFalse(strategy.isExhausted())
    }

    @Test
    fun `isExhausted returns true after max retries`() = runTest {
        val strategy = ReconnectionStrategy(maxRetries = 2)

        strategy.recordAttempt()
        strategy.recordAttempt()

        assertTrue(strategy.isExhausted())
    }

    @Test
    fun `recordAttempt returns correct delay`() = runTest {
        val strategy = ReconnectionStrategy(initialDelayMs = 100)

        val delay = strategy.recordAttempt()

        assertTrue(delay != null)
        assertEquals(100, delay)
    }
}
