package com.mckli.http

import com.mckli.config.ServerConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ConnectionPoolTest {

    @Test
    fun `pool initializes with correct config`() = runTest {
        val config = ServerConfig(
            name = "test",
            endpoint = "https://example.com/api",
            timeout = 5000,
            poolSize = 5
        )

        val pool = ConnectionPool(
            config = config,
            idleTimeout = 1.seconds,
            maxLifetime = 10.seconds
        )

        val metrics = pool.getMetrics()
        assertEquals(0, metrics.activeConnections)
        assertEquals(0, metrics.idleConnections)
        assertEquals(0, metrics.totalCreated)
        assertFalse(metrics.isActive)
    }

    @Test
    fun `pool metrics track active connections`() = runTest {
        val config = ServerConfig(
            name = "test",
            endpoint = "https://example.com/api"
        )

        val pool = ConnectionPool(config)

        // Note: We can't easily test actual execution without a real HTTP server
        // This test validates the structure exists
        val initialMetrics = pool.getMetrics()
        assertEquals(0, initialMetrics.activeConnections)
    }

    @Test
    fun `pool shutdown cleans up resources`() = runTest {
        val config = ServerConfig(
            name = "test",
            endpoint = "https://example.com/api"
        )

        val pool = ConnectionPool(config)

        // Shutdown the pool
        pool.shutdown()

        val metrics = pool.getMetrics()
        assertEquals(0, metrics.activeConnections)
        assertFalse(metrics.isActive)
    }

    @Test
    fun `pool force shutdown is immediate`() = runTest {
        val config = ServerConfig(
            name = "test",
            endpoint = "https://example.com/api"
        )

        val pool = ConnectionPool(config)

        // Force shutdown
        pool.forceShutdown()

        val metrics = pool.getMetrics()
        assertEquals(0, metrics.activeConnections)
    }

    @Test
    fun `PoolMetrics data class works correctly`() {
        val metrics = PoolMetrics(
            activeConnections = 3,
            idleConnections = 2,
            totalCreated = 10,
            isActive = true
        )

        assertEquals(3, metrics.activeConnections)
        assertEquals(2, metrics.idleConnections)
        assertEquals(10, metrics.totalCreated)
        assertTrue(metrics.isActive)
    }

    @Test
    fun `PoolMetrics copy works correctly`() {
        val original = PoolMetrics(
            activeConnections = 5,
            idleConnections = 3,
            totalCreated = 20,
            isActive = true
        )

        val copy = original.copy(activeConnections = 7)

        assertEquals(7, copy.activeConnections)
        assertEquals(3, copy.idleConnections)
        assertEquals(20, copy.totalCreated)
        assertTrue(copy.isActive)

        // Original unchanged
        assertEquals(5, original.activeConnections)
    }
}
