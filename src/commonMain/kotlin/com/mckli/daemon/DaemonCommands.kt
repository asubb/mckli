package com.mckli.daemon

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.mckli.config.ConfigManager
import com.mckli.config.Configuration

class DaemonCommand : CliktCommand(name = "daemon") {
    override fun help(context: Context) = "Manage MCP server daemons"
    init {
        subcommands(
            DaemonStartCommand(),
            DaemonStopCommand(),
            DaemonStatusCommand(),
            DaemonRestartCommand()
        )
    }

    override fun run() {}
}

class DaemonStartCommand : CliktCommand(name = "start") {
    override fun help(context: Context) = "Start a daemon for an MCP server"

    private val serverName by argument(help = "Server name")

    override fun run() {
        val configManager = ConfigManager()
        val config = configManager.readConfig() ?: run {
            echo("No configuration found. Use 'config add' to add a server.", err = true)
            throw com.github.ajalt.clikt.core.CliktError("No configuration")
        }

        val serverConfig = config.servers.find { it.name == serverName } ?: run {
            echo("Server '$serverName' not found in configuration", err = true)
            throw com.github.ajalt.clikt.core.CliktError("Server not found")
        }

        val daemon = DaemonProcess(serverConfig)
        daemon.start().fold(
            onSuccess = {
                echo("Daemon for '$serverName' started successfully")
                echo("PID: ${daemon.getPid()}")
                echo("Socket: ${daemon.getSocketPath()}")
            },
            onFailure = { error ->
                echo("Failed to start daemon: ${error.message}", err = true)
                throw com.github.ajalt.clikt.core.CliktError("Failed to start daemon")
            }
        )
    }
}

class DaemonStopCommand : CliktCommand(name = "stop") {
    override fun help(context: Context) = "Stop a daemon"

    private val serverName by argument(help = "Server name")
    private val force by option("--force", "-f", help = "Force kill daemon").flag(default = false)

    override fun run() {
        val configManager = ConfigManager()
        val config = configManager.readConfig() ?: run {
            echo("No configuration found", err = true)
            throw com.github.ajalt.clikt.core.CliktError("No configuration")
        }

        val serverConfig = config.servers.find { it.name == serverName } ?: run {
            echo("Server '$serverName' not found in configuration", err = true)
            throw com.github.ajalt.clikt.core.CliktError("Server not found")
        }

        val daemon = DaemonProcess(serverConfig)
        daemon.stop(force).fold(
            onSuccess = {
                echo("Daemon for '$serverName' stopped successfully")
            },
            onFailure = { error ->
                echo("Failed to stop daemon: ${error.message}", err = true)
                throw com.github.ajalt.clikt.core.CliktError("Failed to stop daemon")
            }
        )
    }
}

class DaemonStatusCommand : CliktCommand(name = "status") {
    override fun help(context: Context) = "Show status of all daemons"

    override fun run() {
        val configManager = ConfigManager()
        val config = configManager.readConfig()

        if (config == null || config.servers.isEmpty()) {
            echo("No servers configured")
            return
        }

        echo("Daemon status:")
        config.servers.forEach { serverConfig ->
            val daemon = DaemonProcess(serverConfig)
            val isRunning = daemon.isRunning()
            val status = if (isRunning) "RUNNING" else "STOPPED"
            val pid = daemon.getPid()

            echo("  ${serverConfig.name}: $status" + if (pid != null) " (PID: $pid)" else "")

            if (isRunning) {
                echo("    Socket: ${daemon.getSocketPath()}")
                echo("    Transport: ${serverConfig.transport}")

                // Show connection state for SSE transport
                if (serverConfig.transport == com.mckli.config.TransportType.SSE) {
                    // Note: Connection state would need to be retrieved via IPC or status file
                    echo("    SSE Connection: (check daemon logs for connection status)")
                }
            }
        }
    }
}

class DaemonRestartCommand : CliktCommand(name = "restart") {
    override fun help(context: Context) = "Restart a daemon"

    private val serverName by argument(help = "Server name")

    override fun run() {
        val configManager = ConfigManager()
        val config = configManager.readConfig() ?: run {
            echo("No configuration found", err = true)
            throw com.github.ajalt.clikt.core.CliktError("No configuration")
        }

        val serverConfig = config.servers.find { it.name == serverName } ?: run {
            echo("Server '$serverName' not found in configuration", err = true)
            throw com.github.ajalt.clikt.core.CliktError("Server not found")
        }

        val daemon = DaemonProcess(serverConfig)

        // Stop if running
        if (daemon.isRunning()) {
            echo("Stopping daemon...")
            daemon.stop(force = false).onFailure { error ->
                echo("Failed to stop daemon: ${error.message}", err = true)
                throw com.github.ajalt.clikt.core.CliktError("Failed to stop daemon")
            }
        }

        // Start
        echo("Starting daemon...")
        daemon.start().fold(
            onSuccess = {
                echo("Daemon for '$serverName' restarted successfully")
                echo("PID: ${daemon.getPid()}")
            },
            onFailure = { error ->
                echo("Failed to restart daemon: ${error.message}", err = true)
                throw com.github.ajalt.clikt.core.CliktError("Failed to restart daemon")
            }
        )
    }
}
