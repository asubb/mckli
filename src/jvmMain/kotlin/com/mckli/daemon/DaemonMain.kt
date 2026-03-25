package com.mckli.daemon

import com.mckli.config.ConfigManager
import com.mckli.config.TransportType
import com.mckli.http.ConnectionPool
import com.mckli.ipc.UnixSocketServer
import com.mckli.tools.ToolCache
import com.mckli.transport.SseTransport
import com.mckli.transport.TransportFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import java.io.File
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    // Daemon uses its own logger with specific appender in logback.xml
    if (args.isEmpty()) {
        System.err.println("Usage: DaemonMain <server-name>")
        exitProcess(1)
    }

    val serverName = args[0]
    logger.debug { "Daemon entry point hit with serverName: $serverName" }

    runBlocking {
        try {
            val daemon = Daemon(serverName)
            daemon.start()
        } catch (e: Exception) {
            System.err.println("Daemon error: ${e.message}")
            e.printStackTrace()
            logger.error(e) { "Daemon execution failed" }
            exitProcess(1)
        }
    }
}

class Daemon(private val serverName: String) {
    private val configManager = ConfigManager()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var connectionPool: ConnectionPool
    private lateinit var socketServer: UnixSocketServer
    private lateinit var toolCache: ToolCache
    private var sseTransport: SseTransport? = null
    private var isShuttingDown = false

    @Volatile
    private var currentConnectionState = ConnectionState.Disconnected

    @Volatile
    private var lastError: String? = null

    init {
        logger.debug { "Initializing Daemon for server: $serverName" }
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
        println("Starting daemon for server: $serverName")
        logger.info { "Daemon starting for server: $serverName" }

        // Load configuration
        logger.debug { "Loading configuration..." }
        val config = configManager.readConfig()
            ?: throw DaemonException("No configuration found")

        val serverConfig = config.servers.find { it.name == serverName }
            ?: throw DaemonException("Server '$serverName' not found in configuration")

        // Initialize SSE transport if configured
        if (serverConfig.transport == TransportType.SSE) {
            println("Initializing SSE transport...")
            logger.debug { "Initializing SSE transport for server: $serverName" }
            val transport = TransportFactory.create(serverConfig) as SseTransport
            sseTransport = transport

            // Monitor connection state
            scope.launch {
                transport.connectionState.collect { state ->
                    currentConnectionState = when (state) {
                        is com.mckli.transport.SseConnectionState.Disconnected -> ConnectionState.Disconnected
                        is com.mckli.transport.SseConnectionState.Connecting -> ConnectionState.Connecting
                        is com.mckli.transport.SseConnectionState.Connected -> ConnectionState.Connected
                        is com.mckli.transport.SseConnectionState.Reconnecting -> ConnectionState.Reconnecting
                        is com.mckli.transport.SseConnectionState.Failed -> {
                            lastError = state.error
                            ConnectionState.Failed
                        }
                    }
                    println("SSE connection state: $currentConnectionState")
                    logger.debug { "SSE connection state updated: $currentConnectionState" }
                }
            }

            // Establish connection
            currentConnectionState = ConnectionState.Connecting
            logger.debug { "Connecting to SSE endpoint: ${serverConfig.endpoint}" }
            val connectResult = transport.connect()
            if (connectResult.isFailure) {
                lastError = connectResult.exceptionOrNull()?.message
                currentConnectionState = ConnectionState.Failed
                println("Warning: Failed to establish SSE connection: $lastError")
                logger.warn { "Failed to establish SSE connection: $lastError" }
            } else {
                println("SSE connection established")
                logger.debug { "SSE connection established successfully" }
            }
        }

        // Initialize components
        logger.debug { "Initializing connection pool and tool cache" }
        connectionPool = ConnectionPool(serverConfig, sseTransport)
        toolCache = ToolCache(connectionPool)

        // Get socket path
        val daemonProcess = DaemonProcess(serverConfig)
        val socketPath = daemonProcess.getSocketPath()

        // Clean up old socket if exists
        logger.debug { "Ensuring socket path is clear: $socketPath" }
        File(socketPath).delete()

        // Start Unix socket server
        logger.debug { "Starting Unix socket server at $socketPath" }
        socketServer = UnixSocketServer(socketPath, connectionPool, toolCache)
        socketServer.start()

        // Fetch tools on startup
        try {
            logger.debug { "Refreshing tool cache on startup" }
            toolCache.refresh()
            val count = toolCache.getToolCount()
            println("Loaded $count tools from MCP server")
            logger.debug { "Loaded $count tools" }
        } catch (e: Exception) {
            System.err.println("Warning: Failed to load tools: ${e.message}")
            logger.warn(e) { "Failed to load tools: ${e.message}" }
        }

        println("Daemon ready on socket: $socketPath")
        logger.info { "Daemon ready on socket: $socketPath" }

        // Keep daemon alive
        while (!isShuttingDown) {
            Thread.sleep(1000)
        }
    }

    private suspend fun shutdown() {
        if (isShuttingDown) return
        isShuttingDown = true

        println("Shutting down daemon...")
        logger.info { "Shutting down daemon..." }

        try {
            // Close SSE transport gracefully
            logger.debug { "Closing SSE transport" }
            sseTransport?.close()

            logger.debug { "Stopping Unix socket server" }
            socketServer.stop()

            logger.debug { "Shutting down connection pool" }
            connectionPool.shutdown()
        } catch (e: Exception) {
            System.err.println("Error during shutdown: ${e.message}")
            logger.error(e) { "Error during shutdown: ${e.message}" }
        }

        println("Daemon stopped")
        logger.info { "Daemon stopped" }
        exitProcess(0)
    }

    fun getStatus(): DaemonStatus {
        val daemonProcess = DaemonProcess(configManager.readConfig()!!.servers.first { it.name == serverName })
        return DaemonStatus(
            serverName = serverName,
            isRunning = daemonProcess.isRunning(),
            pid = daemonProcess.getPid(),
            socketPath = daemonProcess.getSocketPath(),
            connectionState = currentConnectionState,
            lastError = lastError
        )
    }
}
