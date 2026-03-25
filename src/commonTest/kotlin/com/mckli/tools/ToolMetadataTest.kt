package com.mckli.tools

import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ToolMetadataTest {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    @Test
    fun `ToolMetadata serializes correctly`() {
        val schema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("path", buildJsonObject {
                    put("type", "string")
                    put("description", "File path")
                })
            })
            put("required", buildJsonArray {
                add("path")
            })
        }

        val metadata = ToolMetadata(
            name = "read-file",
            description = "Read a file",
            inputSchema = schema
        )

        val jsonString = json.encodeToString(ToolMetadata.serializer(), metadata)
        val decoded = json.decodeFromString(ToolMetadata.serializer(), jsonString)

        assertEquals("read-file", decoded.name)
        assertEquals("Read a file", decoded.description)
        assertNotNull(decoded.inputSchema)
    }

    @Test
    fun `ToolMetadata without schema serializes correctly`() {
        val metadata = ToolMetadata(
            name = "list-users",
            description = "List all users",
            inputSchema = null
        )

        val jsonString = json.encodeToString(ToolMetadata.serializer(), metadata)
        val decoded = json.decodeFromString(ToolMetadata.serializer(), jsonString)

        assertEquals("list-users", decoded.name)
        assertEquals("List all users", decoded.description)
        assertNull(decoded.inputSchema)
    }

    @Test
    fun `ToolList deserializes from MCP response`() {
        val jsonString = """
            {
                "tools": [
                    {
                        "name": "read-file",
                        "description": "Read file contents",
                        "inputSchema": {
                            "type": "object",
                            "properties": {
                                "path": {"type": "string"}
                            }
                        }
                    },
                    {
                        "name": "write-file",
                        "description": "Write file contents"
                    }
                ]
            }
        """.trimIndent()

        val toolList = json.decodeFromString(ToolList.serializer(), jsonString)

        assertEquals(2, toolList.tools.size)
        assertEquals("read-file", toolList.tools[0].name)
        assertEquals("write-file", toolList.tools[1].name)
        assertNotNull(toolList.tools[0].inputSchema)
        assertNull(toolList.tools[1].inputSchema)
    }

    @Test
    fun `ToolMetadata handles complex schema`() {
        val jsonString = """
            {
                "name": "search",
                "description": "Search files",
                "inputSchema": {
                    "type": "object",
                    "properties": {
                        "query": {"type": "string"},
                        "options": {
                            "type": "object",
                            "properties": {
                                "caseSensitive": {"type": "boolean"},
                                "maxResults": {"type": "integer"}
                            }
                        }
                    },
                    "required": ["query"]
                }
            }
        """.trimIndent()

        val metadata = json.decodeFromString<ToolMetadata>(jsonString)

        assertEquals("search", metadata.name)
        assertNotNull(metadata.inputSchema)

        val schema = metadata.inputSchema as JsonObject
        val properties = schema["properties"] as JsonObject
        assertNotNull(properties["query"])
        assertNotNull(properties["options"])
    }

    @Test
    fun `ToolMetadata with null description`() {
        val jsonString = """
            {
                "name": "no-desc-tool"
            }
        """.trimIndent()

        val metadata = json.decodeFromString<ToolMetadata>(jsonString)

        assertEquals("no-desc-tool", metadata.name)
        assertNull(metadata.description)
        assertNull(metadata.inputSchema)
    }
    
    @Test
    fun `ToolMetadata ignores unknown keys`() {
        val jsonString = """
            {
                "name": "unknown-keys-tool",
                "description": "Tool with unknown keys",
                "title": "Display Title",
                "extra": {
                    "key": "value"
                }
            }
        """.trimIndent()

        val metadata = json.decodeFromString<ToolMetadata>(jsonString)

        assertEquals("unknown-keys-tool", metadata.name)
        assertEquals("Tool with unknown keys", metadata.description)
    }
}
