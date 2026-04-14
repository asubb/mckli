package com.mckli.transport

import com.mckli.config.ServerConfig
import com.mckli.config.TransportType
import com.mckli.http.HttpException
import com.mckli.http.McpRequest
import com.mckli.integration.support.MockSseServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SseTransportReproductionTest {
    private lateinit var mockServer: MockSseServer
    private val port = 8082

    @BeforeTest
    fun setup() {
        mockServer = MockSseServer(port)
        mockServer.start()
    }

    @AfterTest
    fun tearDown() {
        mockServer.stop()
    }

    @Test
    fun `should fail when POSTing to base SSE endpoint if forbidden`() = runBlocking {
        // MockSseServer is configured to return 405 for POST /sse in my previous change
        // but it defaults to sending /messages/ as endpoint event.
        // Let's explicitly disable the session endpoint to force fallback to base endpoint
        mockServer.setSessionEndpoint(null)

        val config = ServerConfig(
            name = "test-server",
            endpoint = "http://localhost:$port/sse",
            transport = TransportType.SSE,
            timeout = 2000 // Short timeout for test
        )

        val transport = SseTransport(config)
        try {
            transport.connect()
            
            // This should wait for 5s and then fail because POST /sse returns 405
            val result = transport.sendRequest(McpRequest(id = "1", method = "initialize", params = null))
            
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is HttpException, "Expected HttpException but got $exception")
            assertEquals(405, (exception as HttpException).statusCode)
        } finally {
            transport.close()
        }
    }

    @Test
    fun `should succeed when POSTing to session-specific endpoint`() = runBlocking {
        // MockSseServer defaults to sessionEndpoint = "/messages/" which returns 200
        mockServer.setSessionEndpoint("/messages/")
        
        val config = ServerConfig(
            name = "test-server",
            endpoint = "http://localhost:$port/sse",
            transport = TransportType.SSE,
            timeout = 5000
        )

        val transport = SseTransport(config)
        try {
            transport.connect()
            
            // Wait a bit for the endpoint event to be processed
            delay(500)

            // This should now use /messages/ and succeed
            val result = transport.sendRequest(McpRequest(id = "init-1", method = "initialize", params = null))
            
            // In MockSseServer, initialize returns a successful response
            assertTrue(result.isSuccess, "Expected success but got ${result.exceptionOrNull()}")
            
            // Verify that /messages/ was used
            assertTrue(mockServer.getRequestCount("/messages/") > 0, "Should have called /messages/")
            assertEquals(0, mockServer.getRequestCount("/sse"), "Should NOT have called /sse with POST")
        } finally {
            transport.close()
        }
    }
}
