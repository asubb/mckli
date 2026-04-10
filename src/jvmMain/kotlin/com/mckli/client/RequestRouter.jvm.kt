package com.mckli.client

import com.mckli.config.ConfigManager
import com.mckli.daemon.DaemonProcess
import com.mckli.daemon.DaemonHttpClient
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonElement

private val logger = KotlinLogging.logger {}

actual class RequestRouter actual constructor(private val serverName: String?) {
    private val configManager = ConfigManager()
    private val daemonClient = DaemonHttpClient()

    actual fun sendMcpRequest(method: String, params: JsonElement?): Result<JsonElement> {
        return Result.failure(RouterException("Direct MCP requests not supported via HTTP API yet"))
    }

    actual fun listTools(filter: String?): Result<JsonElement> {
        return executeRequest { serverName ->
            daemonClient.listTools(serverName, filter).map { 
                kotlinx.serialization.json.Json.encodeToJsonElement(
                    ListSerializer(com.mckli.tools.ToolMetadata.serializer()),
                    it
                )
            }
        }
    }

    actual fun describeTool(toolName: String): Result<JsonElement> {
        return executeRequest { serverName ->
             daemonClient.listTools(serverName).map { tools ->
                val tool = tools.find { it.name == toolName }
                if (tool != null) {
                    kotlinx.serialization.json.Json.encodeToJsonElement(com.mckli.tools.ToolMetadata.serializer(), tool)
                } else {
                    throw RouterException("Tool $toolName not found")
                }
            }
        }
    }

    actual fun callTool(toolName: String, arguments: JsonElement?): Result<JsonElement> {
        return executeRequest { serverName ->
            daemonClient.callTool(serverName, toolName, arguments)
        }
    }

    actual fun refreshTools(): Result<String> {
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
