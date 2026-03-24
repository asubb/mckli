package com.mckli.transport

import com.mckli.config.ServerConfig

expect object TransportFactory {
    fun create(config: ServerConfig): McpTransport
}
