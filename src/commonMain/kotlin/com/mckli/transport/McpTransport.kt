package com.mckli.transport

import com.mckli.http.McpRequest
import com.mckli.http.McpResponse

import kotlinx.coroutines.flow.StateFlow

interface McpTransport {
    suspend fun sendRequest(request: McpRequest): Result<McpResponse>
    fun close()
    val connectionState: StateFlow<SseConnectionState>? get() = null
    suspend fun connect(): Result<Unit> = Result.success(Unit)
}

sealed class SseConnectionState {
    object Disconnected : SseConnectionState()
    object Connecting : SseConnectionState()
    object Connected : SseConnectionState()
    object Reconnecting : SseConnectionState()
    data class Failed(val error: String) : SseConnectionState()
}
