package com.mckli.daemon

import com.mckli.config.Configuration
import com.mckli.config.ServerConfig
import com.mckli.tools.ToolCache
import com.mckli.transport.TransportFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.shared.Transport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
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
        val toolCache: ToolCache,
        val transport: Transport,
        val client: Client,
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
        val connectionState = MutableStateFlow(ConnectionState.Connected)
        val lastError = MutableStateFlow<String?>(null)

        val client = Client(
            clientInfo = Implementation(name = "mckli", version = "1.0.0")
        )

        client.connect(transport)

        val cache = ToolCache(config.name, client)

        // Pre-fetch tools
        scope.launch {
            try {
                cache.refresh()
            } catch (e: Exception) {
                logger.warn(e) { "Failed to pre-fetch tools for ${config.name}" }
            }
        }

        return ServerContext(
            config = config,
            toolCache = cache,
            transport = transport,
            client = client,
            connectionState = connectionState,
            lastError = lastError
        )
    }

    private suspend fun shutdownContext(context: ServerContext) {
        try {
            // context.transport.close() // Transport interface might not have close, but Client.close() should handle it
            context.client.close()
        } catch (e: Exception) {
            logger.error(e) { "Error shutting down context for ${context.config.name}" }
        }
    }

    suspend fun getServerContext(name: String): ServerContext? {
        return mutex.withLock { serverContexts[name] }
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
