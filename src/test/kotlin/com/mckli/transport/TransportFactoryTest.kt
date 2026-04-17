package com.mckli.transport

import com.mckli.config.ServerConfig
import com.mckli.config.TransportType
import kotlin.test.Test
import kotlin.test.assertTrue

class TransportFactoryTest {

    @Test
    fun `create returns HttpTransport for HTTP transport type`() {
        val config = ServerConfig(
            name = "test",
            endpoint = "https://example.com/api",
            transport = TransportType.HTTP
        )

        val transport = TransportFactory.create(config)

        assertTrue(transport is HttpTransport)
    }

    @Test
    fun `create returns HttpTransport when transport not specified`() {
        val config = ServerConfig(
            name = "test",
            endpoint = "https://example.com/api"
        )

        val transport = TransportFactory.create(config)

        assertTrue(transport is HttpTransport)
    }

    @Test
    fun `create returns SseTransport for SSE transport type`() {
        val config = ServerConfig(
            name = "test",
            endpoint = "https://example.com/sse",
            transport = TransportType.SSE
        )

        val transport = TransportFactory.create(config)

        assertTrue(transport is SseTransport)
    }
}
