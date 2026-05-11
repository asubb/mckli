package com.mckli.http

import com.mckli.config.ServerConfig
import com.mckli.transport.TransportFactory
import io.modelcontextprotocol.kotlin.sdk.shared.Transport
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}

/**
 * Connection pool wrapper that manages Transport lifecycle and metrics.
 * For HTTP transport, the actual HTTP connection pooling is handled by Ktor's CIO engine.
 * For SSE transport, a single persistent connection is used.
 */
class ConnectionPool(
    private val config: ServerConfig,
    private val providedTransport: Transport? = null,
    private val idleTimeout: Duration = 5.minutes,
    private val maxLifetime: Duration = 30.minutes
) {
    private val mutex = Mutex()
    private var transport: Transport? = providedTransport
    private var transportCreatedAt: Instant? = Clock.System.now()
    private var lastUsedAt: Instant? = Clock.System.now()
    private var totalCreated = if (providedTransport != null) 1 else 0
    private var activeRequests = 0

    private val metrics = PoolMetrics()

    suspend fun <T> executeRequest(block: suspend (Transport) -> T): T {
        val currentTransport = mutex.withLock {
            activeRequests++
            val now = Clock.System.now()

            // For provided transport (like SSE), we don't recreate it based on time
            if (providedTransport != null) {
                logger.debug { "Using provided transport for ${config.name}" }
                lastUsedAt = now
                return@withLock providedTransport
            }

            // Check if we need to create or recreate the transport
            val needsRecreation = transport == null ||
                (transportCreatedAt?.let { (now - it) > maxLifetime } == true) ||
                (lastUsedAt?.let { (now - it) > idleTimeout } == true)

            if (needsRecreation) {
                logger.debug { "Recreating transport for ${config.name} (reason: ${if (transport == null) "first use" else "expired/idle"})" }
                // transport?.close()
                transport = TransportFactory.create(config)
                transportCreatedAt = now
                totalCreated++
                metrics.totalCreated = totalCreated
            } else {
                logger.debug { "Reusing existing transport for ${config.name}" }
            }

            lastUsedAt = now
            transport!!
        }

        return try {
            block(currentTransport)
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
                isActive = transport != null,
                idleConnections = if (transport != null && activeRequests == 0) 1 else 0
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
            // Only close it if it was NOT provided from outside
            if (providedTransport == null) {
                // transport?.close()
            }
            transport = null
            transportCreatedAt = null
            lastUsedAt = null
            activeRequests = 0
        }
    }

    suspend fun forceShutdown() {
        mutex.withLock {
            if (providedTransport == null) {
                // transport?.close()
            }
            transport = null
            transportCreatedAt = null
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
