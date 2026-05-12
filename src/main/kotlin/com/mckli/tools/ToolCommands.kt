package com.mckli.tools

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.mckli.client.RequestRouter
import com.mckli.config.ConfigManager
import com.mckli.config.Configuration
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

private val logger = KotlinLogging.logger {}

class ToolsCommand : CliktCommand(name = "tools") {
    override fun help(context: Context) = "Manage and invoke MCP tools"

    init {
        subcommands(
            ToolsListCommand(),
            ToolsSearchCommand(),
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
    private val jsonOutput by option("--json", help = "Output in JSON format").flag()
    private val fullOutput by option("--full", "-l", help = "Show full tool descriptions").flag()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = true
    }

    override fun run() = runBlocking {
        val configManager = ConfigManager()
        val config = configManager.readConfig() ?: Configuration()

        val serversToQuery = if (server != null) {
            listOf(server!!)
        } else {
            config.servers.map { it.name }
        }

        if (serversToQuery.isEmpty()) {
            echo("No servers configured", err = true)
            return@runBlocking
        }

        val allTools = mutableMapOf<String, List<ToolMetadata>>()
        var hasError = false

        serversToQuery.forEach { serverName ->
            val router = RequestRouter(serverName)
            router.listTools(filter).fold(
                onSuccess = { result ->
                    try {
                        val tools = json.decodeFromJsonElement(ListSerializer(ToolMetadata.serializer()), result)
                        allTools[serverName] = tools
                    } catch (e: Exception) {
                        logger.error(e) { "Error parsing tools for $serverName: ${e.message}" }
                        hasError = true
                    }
                },
                onFailure = { error ->
                    logger.error { "Error listing tools for $serverName: ${error.message}" }
                    hasError = true
                }
            )
        }

        if (jsonOutput) {
            val jsonResult = allTools.map { (server, tools) ->
                ServerTools(server, tools)
            }
            echo(json.encodeToString(jsonResult))
        } else {
            if (allTools.isEmpty()) {
                echo("No tools available")
            } else {
                allTools.forEach { (serverName, tools) ->
                    echo("Server: $serverName")
                    if (tools.isEmpty()) {
                        echo("  No tools available")
                    } else {
                        tools.forEach { tool ->
                            if (fullOutput) {
                                echo("Tool: ${tool.name}")
                                echo("Description: ${tool.description ?: "No description"}")
                                if (tool.inputSchema != null) {
                                    echo("Input Schema: ${json.encodeToString(tool.inputSchema)}")
                                }
                                echo("-".repeat(40))
                            } else {
                                val desc = tool.getCompactDescription()
                                if (desc != null) {
                                    echo("  ${tool.name.padEnd(20)}  $desc")
                                } else {
                                    echo("  ${tool.name}")
                                }
                            }
                        }
                    }
                    echo("")
                }
            }
            if (hasError) {
                echo("Warning: Some servers could not be queried", err = true)
            }
        }
    }
}

@Serializable
internal data class ServerTools(val server: String, val tools: List<ToolMetadata>)

class ToolsSearchCommand : CliktCommand(name = "search") {
    override fun help(context: Context) = "Search for tools across all servers"

    private val query by argument(help = "Search query")
    private val jsonOutput by option("--json", help = "Output in JSON format").flag()

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val searchService = ToolSearchService(json)

    override fun run() = runBlocking {
        val configManager = ConfigManager()
        val config = configManager.readConfig() ?: Configuration()

        val results = searchService.searchAcrossServers(config.servers, query)

        if (jsonOutput) {
            echo(json.encodeToString(results))
        } else {
            if (results.isEmpty()) {
                echo("No matches found for '$query'")
            } else {
                results.forEach { res ->
                    val previewSnippet = res.preview.lines().firstOrNull() ?: ""
                    val truncatedPreview = if (previewSnippet.length > 80) {
                        previewSnippet.take(77) + "..."
                    } else {
                        previewSnippet
                    }
                    echo("${res.server}:${res.name} $truncatedPreview")
                }
            }
        }
    }
}

class ToolsDescribeCommand : CliktCommand(name = "describe") {
    override fun help(context: Context) = "Show detailed information about a tool"

    private val server by argument(help = "Server name")
    private val toolName by argument(help = "Tool name")

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val prettyJson = Json {
        prettyPrint = true
    }

    override fun run(): Unit = runBlocking {
        logger.info { "Describing tool: $toolName from server: $server" }
        val router = RequestRouter(server)

        router.describeTool(toolName).fold(
            onSuccess = { result ->
                try {
                    val tool = json.decodeFromJsonElement(ToolMetadata.serializer(), result)

                    echo("Tool: ${tool.name}")
                    tool.description?.let { echo("Description: $it") }

                    tool.inputSchema?.let { schema ->
                        echo("\nInput Schema:")
                        echo(prettyJson.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), schema))
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error parsing tool details: ${e.message}. Raw result: $result" }
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

    override fun run() = runBlocking {
        logger.info { "Refreshing tools from server: ${server ?: "default"}" }
        val router = RequestRouter(server)

        router.refreshTools().fold(
            onSuccess = { _ ->
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

    private val prettyJson = Json { prettyPrint = true }

    override fun run() = runBlocking {
        logger.info { "Calling tool: $toolName from server: ${server ?: "default"} with args: $jsonArgs" }
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
                echo(
                    prettyJson.encodeToString(
                        kotlinx.serialization.json.JsonElement.serializer(),
                        result
                    )
                )
            },
            onFailure = { error ->
                echo("Error calling tool: ${error.message}", err = true)
                throw com.github.ajalt.clikt.core.CliktError("Failed to call tool")
            }
        )
    }
}
