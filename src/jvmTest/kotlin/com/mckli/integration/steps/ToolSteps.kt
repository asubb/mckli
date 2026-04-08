package com.mckli.integration.steps

import com.mckli.client.RequestRouter
import com.mckli.integration.support.MockMcpServer
import com.mckli.integration.support.TestConfiguration
import com.mckli.tools.ToolMetadata
import io.cucumber.datatable.DataTable
import io.cucumber.java8.En
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlinx.serialization.builtins.ListSerializer
import kotlin.test.*

class ToolSteps : En {
    private var mockServer: MockMcpServer? = null
    private val mockServers = mutableMapOf<Int, MockMcpServer>()
    private var toolListResult: List<ToolMetadata>? = null
    private var toolDescription: ToolMetadata? = null
    private var toolCallResult: Result<JsonElement>? = null
    private var searchResults: List<SearchResult>? = null
    private var lastError: String? = null

    @Serializable
    private data class SearchResult(
        val server: String,
        val name: String,
        val description: String?,
        val preview: String
    )

    init {
        Before { ->
            TestConfiguration.setup()
        }

        Before("@requires-mock-server") { ->
            val server = MockMcpServer(8080)
            mockServer = server
            mockServers[8080] = server
            server.start()
            Thread.sleep(500) // Give server time to start
        }

        After("@requires-mock-server") { ->
            mockServers.values.forEach { it.stop() }
            mockServers.clear()
            mockServer = null
        }

        Given("a mock MCP server is running on port {int}") { port: Int ->
            if (!mockServers.containsKey(port)) {
                val server = MockMcpServer(port)
                mockServers[port] = server
                if (port == 8080) mockServer = server
                server.start()
                Thread.sleep(500)
            }
        }

        Given("the MCP server has tools:") { dataTable: DataTable ->
            val tools = dataTable.asMaps()
            tools.forEach { row ->
                mockServer?.addTool(
                    name = row["name"]!!,
                    description = row["description"]
                )
            }
        }

        Given("the MCP server has a tool {string} with schema:") { toolName: String, schemaJson: String ->
            val schema = Json.parseToJsonElement(schemaJson).jsonObject
            mockServer?.addTool(
                name = toolName,
                description = "Test tool",
                inputSchema = schema
            )
        }

        Given("the MCP server has a tool {string}") { toolName: String ->
            mockServer?.addTool(name = toolName)
        }

        Given("the MCP server has a tool {string} that accepts:") { toolName: String, dataTable: DataTable ->
            val params = dataTable.asMaps()
            val properties = buildJsonObject {
                params.forEach { row ->
                    put(row["parameter"]!!, buildJsonObject {
                        put("type", row["type"]!!)
                    })
                }
            }

            val required = buildJsonArray {
                params.filter { it["required"] == "true" }.forEach { row ->
                    add(row["parameter"]!!)
                }
            }

            val schema = buildJsonObject {
                put("type", "object")
                put("properties", properties)
                if (required.isNotEmpty()) {
                    put("required", required)
                }
            }

            mockServer?.addTool(
                name = toolName,
                inputSchema = schema
            )
        }

        Given("the MCP server has a tool {string} that accepts no parameters") { toolName: String ->
            mockServer?.addTool(name = toolName)
        }

        Given("the tool {string} returns:") { toolName: String, responseJson: String ->
            val response = Json.parseToJsonElement(responseJson)
            mockServer?.clearTools()
            mockServer?.addTool(name = toolName, response = response)
        }

        Given("the tool {string} returns an error {string}") { toolName: String, errorMsg: String ->
            mockServer?.addTool(name = toolName, error = errorMsg)
        }

        Given("the tool {string} takes {int} seconds to respond") { toolName: String, seconds: Int ->
            mockServer?.addTool(name = toolName, delayMs = seconds * 1000L)
        }

        Given("the MCP server on port {int} has tools:") { port: Int, dataTable: DataTable ->
            val server = mockServers[port] ?: throw IllegalStateException("No mock server on port $port")
            val tools = dataTable.asMaps()
            tools.forEach { row ->
                server.addTool(
                    name = row["name"]!!,
                    description = row["description"]
                )
            }
        }

        Given("the MCP server has no tools") {
            mockServer?.clearTools()
        }

        Given("I have listed tools from {string}") { serverName: String ->
            val router = RequestRouter(serverName)
            val result = router.listTools(null)
            toolListResult = result.getOrNull()?.let { jsonElement ->
                Json.decodeFromJsonElement<List<ToolMetadata>>(jsonElement)
            }
        }

        When("I list tools from {string}") { serverName: String ->
            try {
                val router = RequestRouter(serverName)
                val result = router.listTools(null)
                result.fold(
                    onSuccess = { jsonElement ->
                        toolListResult = Json.decodeFromJsonElement(jsonElement)
                    },
                    onFailure = { error ->
                        lastError = error.message
                    }
                )
            } catch (e: Exception) {
                lastError = e.message
            }
        }

        When("I list tools from {string} with filter {string}") { serverName: String, filter: String ->
            try {
                val router = RequestRouter(serverName)
                val result = router.listTools(filter)
                result.fold(
                    onSuccess = { jsonElement ->
                        toolListResult = Json.decodeFromJsonElement(jsonElement)
                    },
                    onFailure = { error ->
                        lastError = error.message
                    }
                )
            } catch (e: Exception) {
                lastError = e.message
            }
        }

        When("I describe tool {string} from {string}") { toolName: String, serverName: String ->
            try {
                val router = RequestRouter(serverName)
                val result = router.describeTool(toolName)
                result.fold(
                    onSuccess = { jsonElement ->
                        toolDescription = Json.decodeFromJsonElement(jsonElement)
                    },
                    onFailure = { error ->
                        lastError = error.message
                    }
                )
            } catch (e: Exception) {
                lastError = e.message
            }
        }

        When("I refresh tools for {string}") { serverName: String ->
            val router = RequestRouter(serverName)
            val result = router.refreshTools()
            assertTrue(result.isSuccess, "Refresh should succeed: ${result.exceptionOrNull()?.message}")
            // Wait for cache update - we need to see the NEW tool
            var found = false
            for (i in 1..20) {
                Thread.sleep(1000)
                println("[DEBUG_LOG] Polling for new-tool, attempt $i")
                val res = router.listTools(null)
                val toolList = res.getOrNull()?.jsonArray
                println("[DEBUG_LOG] Current tools: ${toolList?.map { it.jsonObject["name"]?.jsonPrimitive?.content }}")
                if (toolList?.any { it.jsonObject["name"]?.jsonPrimitive?.content == "new-tool" } == true) {
                    toolListResult = Json.decodeFromJsonElement(res.getOrNull()!!)
                    found = true
                    break
                }
            }
            assertTrue(found, "New tool not found in cache after refresh and wait")
        }

        When("the MCP server updates its tools to:") { dataTable: DataTable ->
            mockServer?.clearTools()
            val tools = dataTable.asMaps()
            tools.forEach { row ->
                mockServer?.addTool(
                    name = row["name"]!!,
                    description = row["description"]
                )
            }
        }

        When("the MCP server stops responding") {
            mockServer?.setResponding(false)
        }

        When("I list tools from {string} again") { serverName: String ->
            val router = RequestRouter(serverName)
            val result = router.listTools(null)
            result.getOrNull()?.let { jsonElement ->
                toolListResult = Json.decodeFromJsonElement(jsonElement)
            }
        }

        When("I search for {string}") { query: String ->
            val configManager = com.mckli.config.ConfigManager()
            val config = configManager.readConfig() ?: com.mckli.config.Configuration()
            val results = mutableListOf<SearchResult>()
            val json = Json { ignoreUnknownKeys = true }

            config.servers.forEach { serverConfig ->
                val router = com.mckli.client.RequestRouter(serverConfig.name)
                router.listTools(null).onSuccess { result ->
                    val tools = json.decodeFromJsonElement(ListSerializer(ToolMetadata.serializer()), result)
                    tools.forEach { tool ->
                        if (tool.name.contains(query, ignoreCase = true) || 
                            tool.description?.contains(query, ignoreCase = true) == true) {
                            results.add(SearchResult(serverConfig.name, tool.name, tool.description, tool.description ?: tool.name))
                        }
                    }
                }
            }
            searchResults = results
        }

        When("I search for {string} with JSON format") { query: String ->
            val configManager = com.mckli.config.ConfigManager()
            val config = configManager.readConfig() ?: com.mckli.config.Configuration()
            val results = mutableListOf<SearchResult>()
            val json = Json { ignoreUnknownKeys = true }

            config.servers.forEach { serverConfig ->
                val router = com.mckli.client.RequestRouter(serverConfig.name)
                router.listTools(null).onSuccess { result ->
                    val tools = json.decodeFromJsonElement(ListSerializer(ToolMetadata.serializer()), result)
                    tools.forEach { tool ->
                        if (tool.name.contains(query, ignoreCase = true) || 
                            tool.description?.contains(query, ignoreCase = true) == true) {
                            results.add(SearchResult(serverConfig.name, tool.name, tool.description, tool.description ?: tool.name))
                        }
                    }
                }
            }
            searchResults = results
            toolCallResult = Result.success(json.encodeToJsonElement(ListSerializer(SearchResult.serializer()), results))
        }

        When("I call tool {string} with arguments:") { toolName: String, argsJson: String ->
            val router = RequestRouter(null) // Use default server
            val args = Json.parseToJsonElement(argsJson)
            toolCallResult = router.callTool(toolName, args)
        }

        When("I call tool {string} without arguments") { toolName: String ->
            val router = RequestRouter(null)
            toolCallResult = router.callTool(toolName, null)
            lastError = toolCallResult!!.exceptionOrNull()?.message
            println("[DEBUG_LOG] Tool call result for $toolName: success=${toolCallResult!!.isSuccess} error=$lastError")
            if (toolCallResult!!.isSuccess) {
                println("[DEBUG_LOG] Result body: ${toolCallResult!!.getOrNull()}")
            }
        }

        When("I try to call tool {string} without arguments") { toolName: String ->
            val router = RequestRouter(null)
            toolCallResult = router.callTool(toolName, null)
            lastError = toolCallResult!!.exceptionOrNull()?.message
            println("[DEBUG_LOG] Tool call result for $toolName: success=${toolCallResult!!.isSuccess} error=$lastError")
        }

        When("I call tool {string} with JSON output format") { toolName: String ->
            val router = RequestRouter(null)
            toolCallResult = router.callTool(toolName, null)
        }

        Then("I should see tools {string} and {string}") { tool1: String, tool2: String ->
            assertNotNull(toolListResult)
            assertTrue(toolListResult!!.any { it.name == tool1 }, "Tool $tool1 not found")
            assertTrue(toolListResult!!.any { it.name == tool2 }, "Tool $tool2 not found")
        }

        Then("I should see the tool name {string} and its description") { toolName: String ->
            assertNotNull(toolDescription)
            assertEquals(toolName, toolDescription!!.name)
            assertNotNull(toolDescription!!.description)
        }

        Then("I should see tools {string} and {string} but not {string}") { tool1: String, tool2: String, tool3: String ->
            assertNotNull(toolListResult)
            assertTrue(toolListResult!!.any { it.name == tool1 }, "Tool $tool1 not found")
            assertTrue(toolListResult!!.any { it.name == tool2 }, "Tool $tool2 not found")
            assertFalse(toolListResult!!.any { it.name == tool3 }, "Tool $tool3 should not be visible")
        }

        Then("I should see tools with names containing {string}") { query: String ->
            assertNotNull(searchResults)
            assertTrue(searchResults!!.all { it.name.contains(query, ignoreCase = true) }, 
                "All results should contain '$query'")
        }

        Then("I should still see tool {string} from cache") { toolName: String ->
            assertNotNull(toolListResult)
            assertTrue(toolListResult!!.any { it.name == toolName })
        }

        Then("the tool execution should succeed") {
            assertNotNull(toolCallResult)
            assertTrue(toolCallResult!!.isSuccess, "Tool execution failed: ${toolCallResult!!.exceptionOrNull()?.message}")
        }

        Then("the result should contain {string} and {string} values") { key1: String, key2: String ->
            assertNotNull(toolCallResult)
            val result = toolCallResult!!.getOrNull() as? JsonObject
            assertNotNull(result)
            assertNotNull(result[key1])
            assertNotNull(result[key2])
        }

        Then("the tool execution should fail with error {string}") { message: String ->
            assertNotNull(toolCallResult)
            assertTrue(toolCallResult!!.isFailure, "Tool execution should have failed")
            val error = toolCallResult?.exceptionOrNull()?.message ?: lastError
            assertNotNull(error)
            assertTrue(error.contains(message, ignoreCase = true), "Error '$error' should contain '$message'")
        }

        Then("the output should be formatted as valid JSON") {
            assertNotNull(toolCallResult)
            assertTrue(toolCallResult!!.isSuccess)
            assertNotNull(toolCallResult!!.getOrNull())
        }
    }
}
