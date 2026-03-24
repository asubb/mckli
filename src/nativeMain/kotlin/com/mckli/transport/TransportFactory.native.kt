package com.mckli.transport

import com.mckli.config.ServerConfig
import com.mckli.config.TransportType

actual object TransportFactory {
    actual fun create(config: ServerConfig): McpTransport {
        return when (config.transport) {
            TransportType.HTTP -> HttpTransport(config)
            TransportType.SSE -> throw UnsupportedOperationException("SSE transport not yet available on Native platform")
        }
    }
}
