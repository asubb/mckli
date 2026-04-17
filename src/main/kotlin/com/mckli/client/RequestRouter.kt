package com.mckli.client

import com.mckli.config.ConfigManager
import com.mckli.config.ServerConfig
import com.mckli.daemon.DaemonProcess
import com.mckli.daemon.DaemonHttpClient
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.util.UUID

private val logger = KotlinLogging.logger {}

class RequestRouter(private val serverName: String?) {
    private val configManager = ConfigManager()
    private val daemonClient = DaemonHttpClient()

    fun sendMcpRequest(method: String, params: JsonElement?): Result<JsonElement> {
        return Result.failure(RouterException("Direct MCP requests not supported via HTTP API yet"))
    }

    fun listTools(filter: String?): Result<JsonElement> {
        return executeRequest { serverName ->
            daemonClient.listTools(serverName, filter).map { 
                Json.encodeToJsonElement(
                    ListSerializer(com.mckli.tools.ToolMetadata.serializer()),
                    it
                )
            }
        }
    }

    fun describeTool(toolName: String): Result<JsonElement> {
        return executeRequest { serverName ->
             daemonClient.listTools(serverName).map { tools ->
                val tool = tools.find { it.name == toolName }
                if (tool != null) {
                    Json.encodeToJsonElement(com.mckli.tools.ToolMetadata.serializer(), tool)
                } else {
                    throw RouterException("Tool $toolName not found")
                }
            }
        }
    }

    fun callTool(toolName: String, arguments: JsonElement?): Result<JsonElement> {
        return executeRequest { serverName ->
            daemonClient.callTool(serverName, toolName, arguments)
        }
    }

    fun refreshTools(): Result<String> {
        return executeRequest { serverName ->
            daemonClient.refreshTools(serverName)
        }
    }

    private fun <T> executeRequest(
        block: suspend (String) -> Result<T>
    ): Result<T> = try {
        val serverConfig = getServerConfig(serverName, configManager)
        logger.debug { "Routing request to server: ${serverConfig.name}" }
        val daemon = DaemonProcess(serverConfig)

        // Auto-start daemon if not running
        if (!daemon.isRunning()) {
            logger.debug { "Daemon is not running, auto-starting..." }
            daemon.start().onFailure { error ->
                return Result.failure(RouterException("Failed to auto-start daemon: ${error.message}", error))
            }

            // Give daemon time to initialize
            logger.debug { "Waiting for daemon to initialize..." }
            Thread.sleep(2000)
        }

        runBlocking {
            block(serverConfig.name)
        }
    } catch (e: Exception) {
        logger.error(e) { "Unexpected router error" }
        Result.failure(RouterException("Router error: ${e.message}", e))
    }
}

internal fun getServerConfig(serverName: String?, configManager: ConfigManager): ServerConfig {
    val config = configManager.readConfig()
        ?: throw RouterException("No configuration found")

    val selectedName = serverName ?: config.defaultServer
    ?: config.servers.firstOrNull()?.name
    ?: throw RouterException("No server specified and no default configured")

    return config.servers.find { it.name == selectedName }
        ?: throw RouterException("Server '$selectedName' not found")
}

internal fun generateRequestId(): String = UUID.randomUUID().toString()

class RouterException(message: String, cause: Throwable? = null) : Exception(message, cause)
