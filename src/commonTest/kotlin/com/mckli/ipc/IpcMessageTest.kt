package com.mckli.ipc

import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IpcMessageTest {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun `IpcRequest_McpRequest serializes correctly`() {
        val request = IpcRequest.McpRequest(
            requestId = "req-123",
            method = "tools/list",
            params = null
        )

        val jsonString = json.encodeToString(IpcRequest.serializer(), request)
        val decoded = json.decodeFromString<IpcRequest>(jsonString)

        assertTrue(decoded is IpcRequest.McpRequest)
        assertEquals("req-123", decoded.requestId)
        assertEquals("tools/list", decoded.method)
    }

    @Test
    fun `IpcRequest_ListTools serializes correctly`() {
        val request = IpcRequest.ListTools(
            requestId = "req-456",
            filter = "file"
        )

        val jsonString = json.encodeToString(IpcRequest.serializer(), request)
        val decoded = json.decodeFromString<IpcRequest>(jsonString)

        assertTrue(decoded is IpcRequest.ListTools)
        assertEquals("req-456", decoded.requestId)
        assertEquals("file", decoded.filter)
    }

    @Test
    fun `IpcRequest_DescribeTool serializes correctly`() {
        val request = IpcRequest.DescribeTool(
            requestId = "req-789",
            toolName = "read-file"
        )

        val jsonString = json.encodeToString(IpcRequest.serializer(), request)
        val decoded = json.decodeFromString<IpcRequest>(jsonString)

        assertTrue(decoded is IpcRequest.DescribeTool)
        assertEquals("read-file", decoded.toolName)
    }

    @Test
    fun `IpcRequest_CallTool with arguments serializes correctly`() {
        val args = buildJsonObject {
            put("path", "/tmp/file.txt")
            put("encoding", "utf-8")
        }

        val request = IpcRequest.CallTool(
            requestId = "req-call",
            toolName = "read-file",
            arguments = args
        )

        val jsonString = json.encodeToString(IpcRequest.serializer(), request)
        val decoded = json.decodeFromString<IpcRequest>(jsonString)

        assertTrue(decoded is IpcRequest.CallTool)
        assertEquals("read-file", decoded.toolName)
        assertNotNull(decoded.arguments)

        val decodedArgs = decoded.arguments as JsonObject
        assertEquals("/tmp/file.txt", decodedArgs["path"]?.jsonPrimitive?.content)
    }

    @Test
    fun `IpcRequest_RefreshTools serializes correctly`() {
        val request = IpcRequest.RefreshTools(
            requestId = "req-refresh"
        )

        val jsonString = json.encodeToString(IpcRequest.serializer(), request)
        val decoded = json.decodeFromString<IpcRequest>(jsonString)

        assertTrue(decoded is IpcRequest.RefreshTools)
        assertEquals("req-refresh", decoded.requestId)
    }

    @Test
    fun `IpcResponse_Success serializes correctly`() {
        val result = buildJsonObject {
            put("content", "file contents")
            put("size", 1234)
        }

        val response = IpcResponse.Success(
            requestId = "req-123",
            result = result
        )

        val jsonString = json.encodeToString(IpcResponse.serializer(), response)
        val decoded = json.decodeFromString<IpcResponse>(jsonString)

        assertTrue(decoded is IpcResponse.Success)
        assertEquals("req-123", decoded.requestId)

        val decodedResult = decoded.result as JsonObject
        assertEquals("file contents", decodedResult["content"]?.jsonPrimitive?.content)
        assertEquals(1234, decodedResult["size"]?.jsonPrimitive?.int)
    }

    @Test
    fun `IpcResponse_Error serializes correctly`() {
        val response = IpcResponse.Error(
            requestId = "req-456",
            error = "Tool not found",
            details = "Tool 'unknown-tool' does not exist"
        )

        val jsonString = json.encodeToString(IpcResponse.serializer(), response)
        val decoded = json.decodeFromString<IpcResponse>(jsonString)

        assertTrue(decoded is IpcResponse.Error)
        assertEquals("req-456", decoded.requestId)
        assertEquals("Tool not found", decoded.error)
        assertEquals("Tool 'unknown-tool' does not exist", decoded.details)
    }

    @Test
    fun `IpcResponse_Error without details serializes correctly`() {
        val response = IpcResponse.Error(
            requestId = "req-789",
            error = "Internal error",
            details = null
        )

        val jsonString = json.encodeToString(IpcResponse.serializer(), response)
        val decoded = json.decodeFromString<IpcResponse>(jsonString)

        assertTrue(decoded is IpcResponse.Error)
        assertEquals("Internal error", decoded.error)
        assertEquals(null, decoded.details)
    }

    @Test
    fun `Complex IpcRequest round-trip preserves data`() {
        val original = IpcRequest.CallTool(
            requestId = "complex-123",
            toolName = "search",
            arguments = buildJsonObject {
                put("query", "*.kt")
                put("options", buildJsonObject {
                    put("recursive", true)
                    put("maxDepth", 5)
                })
            }
        )

        val jsonString = json.encodeToString(IpcRequest.serializer(), original)
        val decoded = json.decodeFromString<IpcRequest>(jsonString) as IpcRequest.CallTool

        assertEquals(original.requestId, decoded.requestId)
        assertEquals(original.toolName, decoded.toolName)

        val origArgs = original.arguments as JsonObject
        val decodedArgs = decoded.arguments as JsonObject

        assertEquals(
            origArgs["query"]?.jsonPrimitive?.content,
            decodedArgs["query"]?.jsonPrimitive?.content
        )

        val origOptions = origArgs["options"] as JsonObject
        val decodedOptions = decodedArgs["options"] as JsonObject

        assertEquals(
            origOptions["recursive"]?.jsonPrimitive?.boolean,
            decodedOptions["recursive"]?.jsonPrimitive?.boolean
        )
    }
}
