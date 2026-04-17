package com.mckli.transport

import com.mckli.config.ServerConfig
import com.mckli.config.TransportType

object TransportFactory {
    fun create(config: ServerConfig): McpTransport {
        return when (config.transport) {
            TransportType.HTTP -> HttpTransport(config)
            TransportType.SSE -> SseTransport(config)
        }
    }
}
