package com.mckli.transport

import kotlinx.coroutines.flow.StateFlow

interface McpTransport {
    val connectionState: StateFlow<SseConnectionState>? get() = null
    fun close()
}

sealed class SseConnectionState {
    object Disconnected : SseConnectionState()
    object Connecting : SseConnectionState()
    object Connected : SseConnectionState()
    object Reconnecting : SseConnectionState()
    data class Failed(val error: String) : SseConnectionState()
}
