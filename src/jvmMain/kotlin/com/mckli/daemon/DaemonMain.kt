package com.mckli.daemon

import com.mckli.config.ConfigManager
import com.mckli.http.ConnectionPool
import com.mckli.ipc.UnixSocketServer
import com.mckli.tools.ToolCache
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        System.err.println("Usage: DaemonMain <server-name>")
        exitProcess(1)
    }

    val serverName = args[0]

    runBlocking {
        try {
            val daemon = Daemon(serverName)
            daemon.start()
        } catch (e: Exception) {
            System.err.println("Daemon error: ${e.message}")
            e.printStackTrace()
            exitProcess(1)
        }
    }
}

class Daemon(private val serverName: String) {
    private val configManager = ConfigManager()
    private lateinit var connectionPool: ConnectionPool
    private lateinit var socketServer: UnixSocketServer
    private lateinit var toolCache: ToolCache
    private var isShuttingDown = false

    init {
        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(Thread {
            runBlocking {
                shutdown()
            }
        })

        // Handle SIGTERM gracefully
        sun.misc.Signal.handle(sun.misc.Signal("TERM")) {
            runBlocking {
                shutdown()
            }
        }
    }

    suspend fun start() {
        println("Starting daemon for server: $serverName")

        // Load configuration
        val config = configManager.readConfig()
            ?: throw DaemonException("No configuration found")

        val serverConfig = config.servers.find { it.name == serverName }
            ?: throw DaemonException("Server '$serverName' not found in configuration")

        // Initialize components
        connectionPool = ConnectionPool(serverConfig)
        toolCache = ToolCache(connectionPool)

        // Get socket path
        val daemonProcess = DaemonProcess(serverConfig)
        val socketPath = daemonProcess.getSocketPath()

        // Clean up old socket if exists
        File(socketPath).delete()

        // Start Unix socket server
        socketServer = UnixSocketServer(socketPath, connectionPool, toolCache)
        socketServer.start()

        // Fetch tools on startup
        try {
            toolCache.refresh()
            println("Loaded ${toolCache.getToolCount()} tools from MCP server")
        } catch (e: Exception) {
            System.err.println("Warning: Failed to load tools: ${e.message}")
        }

        println("Daemon ready on socket: $socketPath")

        // Keep daemon alive
        while (!isShuttingDown) {
            Thread.sleep(1000)
        }
    }

    private suspend fun shutdown() {
        if (isShuttingDown) return
        isShuttingDown = true

        println("Shutting down daemon...")

        try {
            socketServer.stop()
            connectionPool.shutdown()
        } catch (e: Exception) {
            System.err.println("Error during shutdown: ${e.message}")
        }

        println("Daemon stopped")
        exitProcess(0)
    }
}
