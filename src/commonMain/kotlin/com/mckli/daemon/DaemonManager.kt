package com.mckli.daemon

import com.mckli.config.Configuration
import com.mckli.config.ServerConfig
import com.mckli.config.TransportType
import com.mckli.http.ConnectionPool
import com.mckli.tools.ToolCache
import com.mckli.transport.McpTransport
import com.mckli.transport.SseConnectionState
import com.mckli.transport.TransportFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val logger = KotlinLogging.logger {}

class DaemonManager {
    private val mutex = Mutex()
    private val serverContexts = mutableMapOf<String, ServerContext>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    class ServerContext(
        val config: ServerConfig,
        val connectionPool: ConnectionPool,
        val toolCache: ToolCache,
        val transport: McpTransport,
        val connectionState: MutableStateFlow<ConnectionState>,
        val lastError: MutableStateFlow<String?>
    )

    suspend fun updateConfig(configuration: Configuration) {
        mutex.withLock {
            val currentNames = serverContexts.keys.toSet()
            val newNames = configuration.servers.map { it.name }.toSet()

            // Remove servers no longer in config
            (currentNames - newNames).forEach { name ->
                logger.info { "Removing server context for $name" }
                val context = serverContexts.remove(name)
                context?.let { shutdownContext(it) }
            }

            // Add or update servers
            configuration.servers.forEach { serverConfig ->
                val existing = serverContexts[serverConfig.name]
                if (existing == null) {
                    logger.info { "Adding new server context for ${serverConfig.name}" }
                    serverContexts[serverConfig.name] = createContext(serverConfig)
                } else if (existing.config != serverConfig) {
                    logger.info { "Updating server context for ${serverConfig.name}" }
                    shutdownContext(existing)
                    serverContexts[serverConfig.name] = createContext(serverConfig)
                }
            }
        }
    }

    private suspend fun createContext(config: ServerConfig): ServerContext {
        val transport = TransportFactory.create(config)
        val connectionState = MutableStateFlow(ConnectionState.Disconnected)
        val lastError = MutableStateFlow<String?>(null)

        transport.connectionState?.let { flow ->
            scope.launch {
                flow.collect { state ->
                    connectionState.value = when (state) {
                        is SseConnectionState.Disconnected -> ConnectionState.Disconnected
                        is SseConnectionState.Connecting -> ConnectionState.Connecting
                        is SseConnectionState.Connected -> ConnectionState.Connected
                        is SseConnectionState.Reconnecting -> ConnectionState.Reconnecting
                        is SseConnectionState.Failed -> {
                            lastError.value = state.error
                            ConnectionState.Failed
                        }
                    }
                }
            }
        }

        scope.launch {
            if (config.transport == TransportType.SSE) {
                connectionState.value = ConnectionState.Connecting
            }
            val result = transport.connect()
            if (result.isFailure) {
                lastError.value = result.exceptionOrNull()?.message
                connectionState.value = ConnectionState.Failed
            } else if (config.transport == TransportType.HTTP) {
                connectionState.value = ConnectionState.Connected
            }
        }

        val pool = ConnectionPool(config, transport)
        val cache = ToolCache(pool)

        // Pre-fetch tools
        scope.launch {
            // Wait for transport to be connected if needed
            var count = 0
            while (connectionState.value != ConnectionState.Connected && count < 50) {
                delay(100)
                count++
            }
            try {
                cache.refresh()
            } catch (e: Exception) {
                logger.warn(e) { "Failed to pre-fetch tools for ${config.name}" }
            }
        }

        return ServerContext(
            config = config,
            connectionPool = pool,
            toolCache = cache,
            transport = transport,
            connectionState = connectionState,
            lastError = lastError
        )
    }

    private suspend fun shutdownContext(context: ServerContext) {
        try {
            context.transport.close()
            context.connectionPool.shutdown()
        } catch (e: Exception) {
            logger.error(e) { "Error shutting down context for ${context.config.name}" }
        }
    }

    suspend fun getServerContext(name: String): ServerContext? {
        return mutex.withLock { serverContexts[name] }
    }

    suspend fun listServers(): List<String> {
        return mutex.withLock { serverContexts.keys.toList() }
    }

    suspend fun shutdown() {
        mutex.withLock {
            serverContexts.values.forEach { shutdownContext(it) }
            serverContexts.clear()
        }
        scope.cancel()
    }
    
    suspend fun getStatus(): DaemonStatus {
        return mutex.withLock {
            DaemonStatus(
                isRunning = true,
                pid = null, // To be filled by DaemonMain
                managedServers = serverContexts.keys.toList(),
                connectionStates = serverContexts.mapValues { it.value.connectionState.value },
                lastErrors = serverContexts.mapValues { it.value.lastError.value }
            )
        }
    }
}
