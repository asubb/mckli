package com.mckli.ipc

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
sealed class IpcRequest {
    abstract val requestId: String

    @Serializable
    data class McpRequest(
        override val requestId: String,
        val method: String,
        val params: JsonElement? = null
    ) : IpcRequest()

    @Serializable
    data class ListTools(
        override val requestId: String,
        val filter: String? = null
    ) : IpcRequest()

    @Serializable
    data class DescribeTool(
        override val requestId: String,
        val toolName: String
    ) : IpcRequest()

    @Serializable
    data class CallTool(
        override val requestId: String,
        val toolName: String,
        val arguments: JsonElement?
    ) : IpcRequest()

    @Serializable
    data class RefreshTools(
        override val requestId: String
    ) : IpcRequest()
}

@Serializable
sealed class IpcResponse {
    abstract val requestId: String

    @Serializable
    data class Success(
        override val requestId: String,
        val result: JsonElement
    ) : IpcResponse()

    @Serializable
    data class Error(
        override val requestId: String,
        val error: String,
        val details: String? = null
    ) : IpcResponse()
}
