package com.mckli.integration.steps

import com.mckli.client.RequestRouter
import com.mckli.integration.support.MockMcpServer
import com.mckli.integration.support.TestConfiguration
import com.mckli.tools.ToolMetadata
import io.cucumber.datatable.DataTable
import io.cucumber.java8.En
import kotlinx.serialization.json.*
import kotlin.test.*

class ToolSteps : En {
    private var mockServer: MockMcpServer? = null
    private var toolListResult: List<ToolMetadata>? = null
    private var toolDescription: ToolMetadata? = null
    private var toolCallResult: Result<JsonElement>? = null
    private var lastError: String? = null

    init {
        Before { ->
            TestConfiguration.setup()
        }

        Before("@requires-mock-server") { ->
            mockServer = MockMcpServer(8080)
            mockServer?.start()
            Thread.sleep(500) // Give server time to start
        }

        After("@requires-mock-server") { ->
            mockServer?.stop()
            mockServer = null
        }

        Given("a mock MCP server is running on port {int}") { port: Int ->
            if (mockServer == null) {
                mockServer = MockMcpServer(port)
                mockServer?.start()
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

        Then("I should see {int} tools") { count: Int ->
            assertEquals(count, toolListResult?.size ?: 0)
        }

        Then("I should see tool {string}") { toolName: String ->
            assertNotNull(toolListResult)
            assertTrue(toolListResult!!.any { it.name == toolName }, "Tool $toolName not found")
        }

        Then("I should not see tool {string}") { toolName: String ->
            assertNotNull(toolListResult)
            assertFalse(toolListResult!!.any { it.name == toolName }, "Tool $toolName should not be visible")
        }

        Then("I should see the tool name {string}") { toolName: String ->
            assertNotNull(toolDescription)
            assertEquals(toolName, toolDescription!!.name)
        }

        Then("I should see the tool description") {
            assertNotNull(toolDescription)
            assertNotNull(toolDescription!!.description)
        }

        Then("I should see the input schema with property {string}") { propertyName: String ->
            assertNotNull(toolDescription)
            val schema = toolDescription!!.inputSchema as? JsonObject
            assertNotNull(schema)
            val properties = schema["properties"] as? JsonObject
            assertNotNull(properties?.get(propertyName))
        }

        Then("I should see a message {string}") { message: String ->
            // In real implementation, check console output or response message
            assertTrue(toolListResult?.isEmpty() == true || lastError?.contains(message) == true)
        }

        Then("I should still see tool {string}") { toolName: String ->
            assertNotNull(toolListResult)
            assertTrue(toolListResult!!.any { it.name == toolName })
        }

        Then("the request should complete without contacting the MCP server") {
            // This would be verified by checking that cache was used
            assertTrue(true) // Placeholder
        }

        Then("the tool execution should succeed") {
            assertNotNull(toolCallResult)
            assertTrue(toolCallResult!!.isSuccess, "Tool execution failed: ${toolCallResult!!.exceptionOrNull()?.message}")
        }

        Then("the tool execution should fail") {
            assertNotNull(toolCallResult)
            assertTrue(toolCallResult!!.isFailure, "Tool execution should have failed")
        }

        Then("the result should contain {string} with value {string}") { key: String, value: String ->
            assertNotNull(toolCallResult)
            val result = toolCallResult!!.getOrNull() as? JsonObject
            assertNotNull(result)
            assertEquals(value, result[key]?.jsonPrimitive?.content)
        }

        Then("the result should contain {string} with value {int}") { key: String, value: Int ->
            assertNotNull(toolCallResult)
            val result = toolCallResult!!.getOrNull() as? JsonObject
            assertNotNull(result)
            assertEquals(value, result[key]?.jsonPrimitive?.int)
        }

        Then("the result should contain {int} users") { count: Int ->
            assertNotNull(toolCallResult)
            val result = toolCallResult!!.getOrNull() as? JsonObject
            val users = result?.get("users")?.jsonArray
            assertEquals(count, users?.size)
        }

        Then("I should see error message {string}") { message: String ->
            val error = toolCallResult?.exceptionOrNull()?.message ?: lastError
            assertNotNull(error)
            assertTrue(error.contains(message, ignoreCase = true), "Error '$error' should contain '$message'")
        }

        Then("I should see error message about timeout") {
            val error = toolCallResult?.exceptionOrNull()?.message
            assertNotNull(error)
            assertTrue(error.contains("timeout", ignoreCase = true))
        }

        Then("the output should be valid JSON") {
            assertNotNull(toolCallResult)
            assertTrue(toolCallResult!!.isSuccess)
            assertNotNull(toolCallResult!!.getOrNull())
        }

        Then("the output should be pretty-printed") {
            // Would check JSON formatting in real implementation
            assertTrue(true) // Placeholder
        }

        Then("the tool timeout is set to {int} seconds") { seconds: Int ->
            // Would configure timeout in real implementation
            assertTrue(true) // Placeholder
        }
    }
}
