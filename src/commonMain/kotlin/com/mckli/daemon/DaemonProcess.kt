package com.mckli.daemon

import com.mckli.config.ServerConfig

expect class DaemonProcess(config: ServerConfig) {
    fun start(): Result<Unit>
    fun stop(force: Boolean = false): Result<Unit>
    fun isRunning(): Boolean
    fun getPid(): Int?
    fun getSocketPath(): String
}

data class DaemonStatus(
    val serverName: String,
    val isRunning: Boolean,
    val pid: Int?,
    val socketPath: String
)

class DaemonException(message: String, cause: Throwable? = null) : Exception(message, cause)
