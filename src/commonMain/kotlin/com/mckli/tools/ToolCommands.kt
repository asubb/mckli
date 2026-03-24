package com.mckli.tools

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.option
import com.mckli.client.RequestRouter
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

class ToolsCommand : CliktCommand(name = "tools") {
    override fun help(context: Context) = "Manage and invoke MCP tools"
    init {
        subcommands(
            ToolsListCommand(),
            ToolsDescribeCommand(),
            ToolsRefreshCommand(),
            ToolsCallCommand()
        )
    }

    override fun run() {}
}

class ToolsListCommand : CliktCommand(name = "list") {
    override fun help(context: Context) = "List available tools"

    private val server by argument(help = "Server name").optional()
    private val filter by option("--filter", "-f", help = "Filter tools by name or description")

    override fun run() {
        val router = RequestRouter(server)

        router.listTools(filter).fold(
            onSuccess = { result ->
                try {
                    val tools = Json.decodeFromJsonElement(ListSerializer(ToolMetadata.serializer()), result)

                    if (tools.isEmpty()) {
                        echo("No tools available")
                    } else {
                        echo("Available tools (${tools.size}):")
                        tools.forEach { tool ->
                            echo("  ${tool.name}")
                            tool.description?.let { desc ->
                                echo("    $desc")
                            }
                        }
                    }
                } catch (e: Exception) {
                    echo("Error parsing tools: ${e.message}", err = true)
                    throw com.github.ajalt.clikt.core.CliktError("Failed to parse tools")
                }
            },
            onFailure = { error ->
                echo("Error listing tools: ${error.message}", err = true)
                throw com.github.ajalt.clikt.core.CliktError("Failed to list tools")
            }
        )
    }
}

class ToolsDescribeCommand : CliktCommand(name = "describe") {
    override fun help(context: Context) = "Show detailed information about a tool"

    private val server by argument(help = "Server name").optional()
    private val toolName by argument(help = "Tool name")

    override fun run() {
        val router = RequestRouter(server)

        router.describeTool(toolName).fold(
            onSuccess = { result ->
                try {
                    val tool = Json.decodeFromJsonElement(ToolMetadata.serializer(), result)

                    echo("Tool: ${tool.name}")
                    tool.description?.let { echo("Description: $it") }

                    tool.inputSchema?.let { schema ->
                        echo("\nInput Schema:")
                        echo(Json { prettyPrint = true }.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), schema))
                    }
                } catch (e: Exception) {
                    echo("Error parsing tool details: ${e.message}", err = true)
                    throw com.github.ajalt.clikt.core.CliktError("Failed to parse tool details")
                }
            },
            onFailure = { error ->
                echo("Error describing tool: ${error.message}", err = true)
                throw com.github.ajalt.clikt.core.CliktError("Failed to describe tool")
            }
        )
    }
}

class ToolsRefreshCommand : CliktCommand(name = "refresh") {
    override fun help(context: Context) = "Refresh tool cache from MCP server"

    private val server by argument(help = "Server name").optional()

    override fun run() {
        val router = RequestRouter(server)

        router.refreshTools().fold(
            onSuccess = { result ->
                echo("Tools refreshed successfully")
            },
            onFailure = { error ->
                echo("Error refreshing tools: ${error.message}", err = true)
                throw com.github.ajalt.clikt.core.CliktError("Failed to refresh tools")
            }
        )
    }
}

class ToolsCallCommand : CliktCommand(name = "call") {
    override fun help(context: Context) = "Invoke an MCP tool"

    private val server by argument(help = "Server name").optional()
    private val toolName by argument(help = "Tool name")
    private val jsonArgs by option("--json", "-j", help = "Tool arguments as JSON")

    override fun run() {
        val router = RequestRouter(server)

        val arguments = jsonArgs?.let { jsonStr ->
            try {
                Json.parseToJsonElement(jsonStr)
            } catch (e: Exception) {
                echo("Invalid JSON: ${e.message}", err = true)
                throw com.github.ajalt.clikt.core.CliktError("Invalid JSON arguments")
            }
        }

        router.callTool(toolName, arguments).fold(
            onSuccess = { result ->
                echo(Json { prettyPrint = true }.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), result))
            },
            onFailure = { error ->
                echo("Error calling tool: ${error.message}", err = true)
                throw com.github.ajalt.clikt.core.CliktError("Failed to call tool")
            }
        )
    }
}
