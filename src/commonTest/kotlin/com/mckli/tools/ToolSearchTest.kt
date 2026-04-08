package com.mckli.tools

import kotlinx.serialization.json.*
import kotlinx.serialization.builtins.ListSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToolSearchTest {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    @Test
    fun `SearchResult serializes correctly`() {
        val result = SearchResult(
            server = "test-server",
            name = "test-tool",
            description = "A test tool",
            preview = "A test tool preview"
        )

        val jsonString = json.encodeToString(SearchResult.serializer(), result)
        val decoded = json.decodeFromString(SearchResult.serializer(), jsonString)

        assertEquals("test-server", decoded.server)
        assertEquals("test-tool", decoded.name)
        assertEquals("A test tool", decoded.description)
        assertEquals("A test tool preview", decoded.preview)
    }

    @Test
    fun `SearchResult with null description serializes correctly`() {
        val result = SearchResult(
            server = "test-server",
            name = "test-tool",
            description = null,
            preview = "test-tool"
        )

        val jsonString = json.encodeToString(SearchResult.serializer(), result)
        val decoded = json.decodeFromString(SearchResult.serializer(), jsonString)

        assertEquals("test-server", decoded.server)
        assertEquals("test-tool", decoded.name)
        assertEquals(null, decoded.description)
        assertEquals("test-tool", decoded.preview)
    }

    @Test
    fun `ServerTools serializes correctly`() {
        val tools = listOf(
            ToolMetadata(name = "tool1", description = "desc1"),
            ToolMetadata(name = "tool2", description = "desc2")
        )
        val serverTools = ServerTools(server = "server1", tools = tools)

        val jsonString = json.encodeToString(ServerTools.serializer(), serverTools)
        val decoded = json.decodeFromString(ServerTools.serializer(), jsonString)

        assertEquals("server1", decoded.server)
        assertEquals(2, decoded.tools.size)
        assertEquals("tool1", decoded.tools[0].name)
        assertEquals("tool2", decoded.tools[1].name)
    }

    @Test
    fun `filterTools should correctly filter and format results`() {
        val tools = listOf(
            ToolMetadata(name = "read-file", description = "Read content from a file"),
            ToolMetadata(name = "write-file", description = "Write content to a file"),
            ToolMetadata(name = "list-tools", description = "List all available tools")
        )
        val service = ToolSearchService()
        val results = service.filterTools("test-server", tools, "file")

        assertEquals(2, results.size)
        assertTrue(results.any { it.name == "read-file" })
        assertTrue(results.any { it.name == "write-file" })

        val readFileResult = results.find { it.name == "read-file" }!!
        assertEquals("Read content from a file", readFileResult.preview)
    }

    @Test
    fun `filterTools should be case insensitive`() {
        val tools = listOf(
            ToolMetadata(name = "Read-File", description = "READ content")
        )
        val service = ToolSearchService()
        val results = service.filterTools("test-server", tools, "read")

        assertEquals(1, results.size)
        assertEquals("Read-File", results[0].name)
    }

    @Test
    fun `filterTools should use blank string as preview if description doesn't match`() {
        val tools = listOf(
            ToolMetadata(name = "test-tool", description = "Something else")
        )
        val service = ToolSearchService()
        val results = service.filterTools("test-server", tools, "test")

        assertEquals(1, results.size)
        assertEquals("", results[0].preview)
    }

    @Test
    fun `filterTools should use description as preview if description matches`() {
        val tools = listOf(
            ToolMetadata(name = "tool", description = "This is a test")
        )
        val service = ToolSearchService()
        val results = service.filterTools("test-server", tools, "test")

        assertEquals(1, results.size)
        assertEquals("This is a test", results[0].preview)
    }
}
