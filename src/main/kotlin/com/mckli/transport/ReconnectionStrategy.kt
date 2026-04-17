package com.mckli.transport

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min
import kotlin.math.pow

class ReconnectionStrategy(
    private val initialDelayMs: Long = 1000,
    private val maxDelayMs: Long = 30000,
    private val maxRetries: Int = 10,
    private val backoffMultiplier: Double = 2.0
) {
    private val attemptCount = AtomicInteger(0)

    /**
     * Calculate the delay for the next reconnection attempt.
     * Returns null if max retries exceeded.
     */
    fun getNextDelay(): Long? {
        val currentAttempt = attemptCount.get()

        if (currentAttempt >= maxRetries) {
            return null // Max retries exceeded
        }

        // Calculate exponential backoff: initialDelay * (multiplier ^ attempt)
        val calculatedDelay = (initialDelayMs * backoffMultiplier.pow(currentAttempt.toDouble())).toLong()

        // Cap at maxDelayMs
        return min(calculatedDelay, maxDelayMs)
    }

    /**
     * Record a reconnection attempt and return the delay to wait before next attempt.
     * Returns null if max retries exceeded.
     */
    suspend fun recordAttempt(): Long? {
        val delay = getNextDelay() ?: return null

        attemptCount.incrementAndGet()
        delay(delay)

        return delay
    }

    /**
     * Reset the strategy after successful reconnection.
     */
    fun reset() {
        attemptCount.set(0)
    }

    /**
     * Get the current attempt count.
     */
    fun getAttemptCount(): Int = attemptCount.get()

    /**
     * Check if max retries have been exceeded.
     */
    fun isExhausted(): Boolean = attemptCount.get() >= maxRetries
}
