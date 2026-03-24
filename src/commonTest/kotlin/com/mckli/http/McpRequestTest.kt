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

        val error = response.error!!
        assertEquals(-32600, error.code)
        assertEquals("Invalid request", error.message)
        assertNotNull(error.data)
    }

    @Test
    fun `McpError serializes and deserializes`() {
        val error = McpError(
            code = -32601,
            message = "Method not found",
            data = JsonPrimitive("tools/unknown")
        )

        val jsonString = json.encodeToString(McpError.serializer(), error)
        val decoded = json.decodeFromString<McpError>(jsonString)

        assertEquals(-32601, decoded.code)
        assertEquals("Method not found", decoded.message)
        assertEquals("tools/unknown", decoded.data?.jsonPrimitive?.content)
    }
}
