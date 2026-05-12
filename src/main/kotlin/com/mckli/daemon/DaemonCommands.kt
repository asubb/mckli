package com.mckli.daemon

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.mckli.config.ServerConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking

private val logger = KotlinLogging.logger {}

class DaemonCommand : CliktCommand(name = "daemon") {
    override fun help(context: Context) = "Manage the unified MCP server daemon"

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

private fun getDummyConfig(): ServerConfig {
    return ServerConfig(name = "daemon", endpoint = "http://localhost:5030")
}

class DaemonStartCommand : CliktCommand(name = "start") {
    override fun help(context: Context) = "Start the unified daemon"

    override fun run() = runBlocking {
        logger.info { "Starting unified daemon" }
        val daemon = DaemonProcess(getDummyConfig())
        daemon.start().fold(
            onSuccess = {
                echo("Unified daemon started successfully")
                echo("PID: ${daemon.getPid()}")
            },
            onFailure = { error ->
                echo("Failed to start daemon: ${error.message}", err = true)
                throw com.github.ajalt.clikt.core.CliktError("Failed to start daemon")
            }
        )
    }
}

class DaemonStopCommand : CliktCommand(name = "stop") {
    override fun help(context: Context) = "Stop the unified daemon"

    private val force by option("--force", "-f", help = "Force kill daemon").flag(default = false)

    override fun run() {
        logger.info { "Stopping unified daemon (force=$force)" }
        val daemon = DaemonProcess(getDummyConfig())
        daemon.stop(force).fold(
            onSuccess = {
                echo("Unified daemon stopped successfully")
            },
            onFailure = { error ->
                echo("Failed to stop daemon: ${error.message}", err = true)
                throw com.github.ajalt.clikt.core.CliktError("Failed to stop daemon")
            }
        )
    }
}

class DaemonStatusCommand : CliktCommand(name = "status") {
    override fun help(context: Context) = "Show status of the unified daemon"

    override fun run() {
        logger.info { "Checking unified daemon status" }
        val daemon = DaemonProcess(getDummyConfig())
        val isRunning = daemon.isRunning()
        val pid = daemon.getPid()

        if (isRunning) {
            echo("Unified daemon: RUNNING" + (if (pid != null) " (PID: $pid)" else ""))

            // Try to get detailed status via HTTP
            runBlocking {
                val client = DaemonHttpClient()
                client.getHealth().onSuccess { status ->
                    echo("Managed servers: ${status.managedServers.joinToString(", ")}")
                    status.connectionStates.forEach { (name, state) ->
                        val error = status.lastErrors[name]
                        echo("  - $name: $state" + (if (error != null) " (Error: $error)" else ""))
                    }
                }.onFailure {
                    echo("Could not retrieve detailed status from daemon HTTP API")
                }
            }
        } else {
            echo("Unified daemon: STOPPED")
        }
    }
}

class DaemonRestartCommand : CliktCommand(name = "restart") {
    override fun help(context: Context) = "Restart the unified daemon"

    override fun run() = runBlocking {
        logger.info { "Restarting unified daemon" }
        val daemon = DaemonProcess(getDummyConfig())

        if (daemon.isRunning()) {
            echo("Stopping daemon...")
            daemon.stop(force = false).onFailure { error ->
                echo("Failed to stop daemon: ${error.message}", err = true)
                throw com.github.ajalt.clikt.core.CliktError("Failed to stop daemon")
            }
        }

        echo("Starting daemon...")
        daemon.start().fold(
            onSuccess = {
                echo("Unified daemon restarted successfully")
                echo("PID: ${daemon.getPid()}")
            },
            onFailure = { error ->
                echo("Failed to restart daemon: ${error.message}", err = true)
                throw com.github.ajalt.clikt.core.CliktError("Failed to restart daemon")
            }
        )
    }
}
