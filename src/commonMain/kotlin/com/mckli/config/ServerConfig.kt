package com.mckli.config

import kotlinx.serialization.Serializable

@Serializable
enum class TransportType {
    HTTP,
    SSE
}

@Serializable
data class ServerConfig(
    val name: String,
    val endpoint: String,
    val transport: TransportType = TransportType.HTTP,
    val auth: AuthConfig? = null,
    val timeout: Long = 30000, // milliseconds
    val poolSize: Int = 10
)

@Serializable
sealed class AuthConfig {
    @Serializable
    data class Basic(val username: String, val password: String) : AuthConfig()

    @Serializable
    data class Bearer(val token: String) : AuthConfig()
}

@Serializable
data class Configuration(
    val servers: List<ServerConfig> = emptyList(),
    val defaultServer: String? = null
)
