package com.mckli.client

import com.mckli.config.ConfigManager
import com.mckli.config.ServerConfig
import kotlinx.serialization.json.JsonElement
import java.util.UUID

expect class RequestRouter(serverName: String?) {
    fun sendMcpRequest(method: String, params: JsonElement?): Result<JsonElement>
    fun listTools(filter: String?): Result<JsonElement>
    fun describeTool(toolName: String): Result<JsonElement>
    fun callTool(toolName: String, arguments: JsonElement?): Result<JsonElement>
    fun refreshTools(): Result<String>
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
