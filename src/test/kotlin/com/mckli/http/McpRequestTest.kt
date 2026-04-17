package com.mckli.http

import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class McpRequestTest {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun `McpRequest serializes correctly`() {
        val request = McpRequest(
            id = "123",
            method = "tools/list",
            params = null
        )

        val jsonString = json.encodeToString(McpRequest.serializer(), request)
        val decoded = json.decodeFromString<McpRequest>(jsonString)

        assertEquals("2.0", decoded.jsonrpc)
        assertEquals("123", decoded.id)
        assertEquals("tools/list", decoded.method)
        assertNull(decoded.params)
    }

    @Test
    fun `McpRequest with params serializes correctly`() {
        val params = buildJsonObject {
            put("name", "test-tool")
            put("arguments", buildJsonObject {
                put("path", "/tmp/file")
            })
        }

        val request = McpRequest(
            id = "456",
            method = "tools/call",
            params = params
        )

        val jsonString = json.encodeToString(McpRequest.serializer(), request)
        val decoded = json.decodeFromString<McpRequest>(jsonString)

        assertEquals("456", decoded.id)
        assertEquals("tools/call", decoded.method)
        assertNotNull(decoded.params)

        val decodedParams = decoded.params as JsonObject
        assertEquals("test-tool", decodedParams["name"]?.jsonPrimitive?.content)
    }

    @Test
    fun `McpResponse with result parses correctly`() {
        val jsonString = """
            {
                "jsonrpc": "2.0",
                "id": "123",
                "result": {
                    "tools": [
                        {"name": "tool1"},
                        {"name": "tool2"}
                    ]
                }
            }
        """.trimIndent()

        val response = json.decodeFromString<McpResponse>(jsonString)

        assertEquals("2.0", response.jsonrpc)
        assertEquals("123", response.id)
        assertNotNull(response.result)
        assertNull(response.error)

        val result = response.result as JsonObject
        val tools = result["tools"]?.jsonArray
        assertEquals(2, tools?.size)
    }

    @Test
    fun `McpResponse with error parses correctly`() {
        val jsonString = """
            {
                "jsonrpc": "2.0",
                "id": "123",
                "error": {
                    "code": -32600,
                    "message": "Invalid request",
                    "data": {"detail": "Missing required field"}
                }
            }
        """.trimIndent()

        val response = json.decodeFromString<McpResponse>(jsonString)

        assertEquals("123", response.id)
        assertNull(response.result)
        assertNotNull(response.error)

        assertEquals(-32600, response.error.code)
        assertEquals("Invalid request", response.error.message)
        assertNotNull(response.error.data)
    }

    @Test
    fun `McpNotification parses correctly`() {
        val jsonString = """
            {
                "jsonrpc": "2.0",
                "method": "notifications/message",
                "params": {
                    "level": "info",
                    "logger": "server",
                    "data": "Hello"
                }
            }
        """.trimIndent()

        val notification = json.decodeFromString<McpNotification>(jsonString)

        assertEquals("2.0", notification.jsonrpc)
        assertEquals("notifications/message", notification.method)
        assertNotNull(notification.params)
    }

    @Test
    fun `McpResponse without id parses correctly`() {
        val jsonString = """
            {
                "jsonrpc": "2.0",
                "method": "notifications/message",
                "params": {
                    "level": "info",
                    "logger": "server",
                    "data": "Hello"
                }
            }
        """.trimIndent()

        val response = json.decodeFromString<McpResponse>(jsonString)

        assertEquals("2.0", response.jsonrpc)
        assertNull(response.id)
        assertEquals("notifications/message", response.method)
        assertNull(response.result)
    }

    @Test
    fun `McpRequest serializes with jsonrpc field`() {
        val jsonWithDefaults = Json { encodeDefaults = true }
        val request = McpRequest(
            id = "123",
            method = "tools/list"
        )

        val jsonString = jsonWithDefaults.encodeToString(McpRequest.serializer(), request)
        assertNotNull(jsonString)
        val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
        assertEquals("2.0", jsonObject["jsonrpc"]?.jsonPrimitive?.content)
    }

    @Test
    fun `McpNotification serializes correctly without id`() {
        val jsonWithDefaults = Json { encodeDefaults = true }
        val notification = McpNotification(
            method = "notifications/initialized"
        )

        val jsonString = jsonWithDefaults.encodeToString(McpNotification.serializer(), notification)
        val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
        
        assertEquals("2.0", jsonObject["jsonrpc"]?.jsonPrimitive?.content)
        assertEquals("notifications/initialized", jsonObject["method"]?.jsonPrimitive?.content)
        assertNull(jsonObject["id"], "Notification should NOT have an id field")
    }
}
