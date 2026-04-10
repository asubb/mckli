package com.mckli.daemon

import com.mckli.config.ConfigManager
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

@Serializable
data class ToolCallRequest(
    val toolName: String,
    val arguments: JsonElement? = null
)

class UnifiedDaemon {
    private val configManager = ConfigManager()
    private val daemonManager = DaemonManager()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isShuttingDown = false

    init {
        logger.debug { "Initializing Unified Daemon" }
        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(Thread {
            runBlocking {
                logger.debug { "Shutdown hook triggered" }
                shutdown()
            }
        })

        // Handle SIGTERM gracefully
        try {
            sun.misc.Signal.handle(sun.misc.Signal("TERM")) {
                runBlocking {
                    logger.debug { "SIGTERM signal received" }
                    shutdown()
                }
            }
        } catch (e: Exception) {
            logger.warn { "Failed to register SIGTERM handler: ${e.message}" }
        }
    }

    suspend fun start() {
        println("Starting unified daemon")
        logger.info { "Unified daemon starting" }

        // Load configuration
        logger.debug { "Loading configuration..." }
        val config = configManager.readConfig()
            ?: throw DaemonException("No configuration found")

        // Initialize all servers from config
        daemonManager.updateConfig(config)

        println("Unified daemon ready")
        logger.info { "Unified daemon ready" }

        // Keep daemon alive
        while (!isShuttingDown) {
            delay(1000)
        }
    }

    suspend fun shutdown() {
        if (isShuttingDown) return
        isShuttingDown = true

        println("Shutting down unified daemon...")
        logger.info { "Shutting down unified daemon..." }

        try {
            daemonManager.shutdown()
        } catch (e: Exception) {
            System.err.println("Error during shutdown: ${e.message}")
            logger.error(e) { "Error during shutdown: ${e.message}" }
        }

        println("Unified daemon stopped")
        logger.info { "Unified daemon stopped" }
        exitProcess(0)
    }

    suspend fun getStatus(): DaemonStatus {
        return daemonManager.getStatus()
    }
    
    fun getDaemonManager() = daemonManager
}
