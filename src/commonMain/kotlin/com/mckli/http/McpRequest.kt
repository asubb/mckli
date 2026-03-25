package com.mckli.http

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

@Serializable
sealed interface McpMessage {
    val jsonrpc: String
    val method: String
    val params: JsonElement?
}

@Serializable
data class McpRequest(
    override val jsonrpc: String = "2.0",
    val id: String,
    override val method: String,
    override val params: JsonElement? = null
) : McpMessage

@Serializable
data class McpNotification(
    override val jsonrpc: String = "2.0",
    override val method: String,
    override val params: JsonElement? = null
) : McpMessage

@Serializable
data class McpInitializeParams(
    val protocolVersion: String,
    val capabilities: JsonObject = buildJsonObject {},
    val clientInfo: McpClientInfo
)

@Serializable
data class McpClientInfo(
    val name: String,
    val version: String
)

@Serializable
data class McpResponse(
    val jsonrpc: String = "2.0",
    val id: String? = null,
    val method: String? = null,
    val result: JsonElement? = null,
    val error: McpError? = null
)

@Serializable
data class McpInitializeResult(
    val protocolVersion: String,
    val capabilities: JsonObject,
    val serverInfo: McpServerInfo
)

@Serializable
data class McpServerInfo(
    val name: String,
    val version: String
)

@Serializable
data class McpError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null
)
