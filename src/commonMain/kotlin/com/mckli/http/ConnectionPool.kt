package com.mckli.http

import com.mckli.config.ServerConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.milliseconds

/**
 * Connection pool wrapper that manages HttpMcpClient lifecycle and metrics.
 * The actual HTTP connection pooling is handled by Ktor's CIO engine.
 */
class ConnectionPool(
    private val config: ServerConfig,
    private val idleTimeout: Duration = 5.minutes,
    private val maxLifetime: Duration = 30.minutes
) {
    private val mutex = Mutex()
    private var client: HttpMcpClient? = null
    private var clientCreatedAt: Instant? = null
    private var lastUsedAt: Instant? = null
    private var totalCreated = 0
    private var activeRequests = 0

    private val metrics = PoolMetrics()

    suspend fun <T> executeRequest(block: suspend (HttpMcpClient) -> T): T {
        val currentClient = mutex.withLock {
            activeRequests++
            val now = Clock.System.now()

            // Check if we need to create or recreate the client
            val needsRecreation = client == null ||
                (clientCreatedAt?.let { (now - it) > maxLifetime } == true) ||
                (lastUsedAt?.let { (now - it) > idleTimeout } == true)

            if (needsRecreation) {
                client?.close()
                client = HttpMcpClient(config)
                clientCreatedAt = now
                totalCreated++
                metrics.totalCreated = totalCreated
            }

            lastUsedAt = now
            client!!
        }

        return try {
            block(currentClient)
        } finally {
            mutex.withLock {
                activeRequests--
                metrics.activeConnections = activeRequests
            }
        }
    }

    suspend fun getMetrics(): PoolMetrics {
        return mutex.withLock {
            metrics.copy(
                isActive = client != null,
                idleConnections = if (client != null && activeRequests == 0) 1 else 0
            )
        }
    }

    suspend fun checkForLeaks() {
        val currentMetrics = getMetrics()
        if (currentMetrics.activeConnections >= config.poolSize * 0.8) {
            // Log warning about potential connection leak
            println("WARNING: Connection pool for '${config.name}' is at ${currentMetrics.activeConnections}/${config.poolSize} (potential leak)")
        }
    }

    suspend fun shutdown() {
        mutex.withLock {
            client?.close()
            client = null
            clientCreatedAt = null
            lastUsedAt = null
            activeRequests = 0
        }
    }

    suspend fun forceShutdown() {
        mutex.withLock {
            client?.close()
            client = null
            clientCreatedAt = null
            lastUsedAt = null
            activeRequests = 0
        }
    }
}

data class PoolMetrics(
    var activeConnections: Int = 0,
    var idleConnections: Int = 0,
    var totalCreated: Int = 0,
    var isActive: Boolean = false
)
