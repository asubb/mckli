package com.mckli.daemon

import com.mckli.config.ServerConfig

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

expect class DaemonProcess(config: ServerConfig) {
    fun start(): Result<Unit>
    fun stop(force: Boolean = false): Result<Unit>
    fun isRunning(): Boolean
    fun getPid(): Int?
    fun getSocketPath(): String
}

@Serializable
data class DaemonStatus(
    val isRunning: Boolean,
    val pid: Int?,
    val managedServers: List<String> = emptyList(),
    val connectionStates: Map<String, ConnectionState> = emptyMap(),
    val lastErrors: Map<String, String?> = emptyMap()
)

enum class ConnectionState {
    Unknown,
    Disconnected,
    Connecting,
    Connected,
    Reconnecting,
    Failed
}

class DaemonException(message: String, cause: Throwable? = null) : Exception(message, cause)
