package com.mckli.transport

import com.mckli.config.ServerConfig
import com.mckli.config.TransportType
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import kotlin.test.Test
import kotlin.test.assertTrue

class TransportFactoryTest {

    @Test
    fun `create returns StreamableHttpClientTransport for HTTP transport type`() {
        val config = ServerConfig(
            name = "test",
            endpoint = "https://example.com/api",
            transport = TransportType.HTTP
        )

        val transport = TransportFactory.create(config)

        assertTrue(transport is StreamableHttpClientTransport)
    }

    @Test
    fun `create returns StreamableHttpClientTransport when transport not specified`() {
        val config = ServerConfig(
            name = "test",
            endpoint = "https://example.com/api"
        )

        val transport = TransportFactory.create(config)

        assertTrue(transport is StreamableHttpClientTransport)
    }

    @Test
    fun `create returns SseClientTransport for SSE transport type`() {
        val config = ServerConfig(
            name = "test",
            endpoint = "https://example.com/sse",
            transport = TransportType.SSE
        )

        val transport = TransportFactory.create(config)

        assertTrue(transport is SseClientTransport)
    }
}
