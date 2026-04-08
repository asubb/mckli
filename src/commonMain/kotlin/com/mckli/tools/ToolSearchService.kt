package com.mckli.tools

import com.mckli.client.RequestRouter
import com.mckli.config.ServerConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.math.max
import kotlin.math.min

private val logger = KotlinLogging.logger {}

@Serializable
internal data class SearchResult(
    val server: String,
    val name: String,
    val description: String?,
    val preview: String
)

internal class ToolSearchService(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
) {
    fun searchAcrossServers(servers: List<ServerConfig>, query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        servers.forEach { serverConfig ->
            val router = RequestRouter(serverConfig.name)
            router.listTools(null).onSuccess { result ->
                try {
                    val tools = json.decodeFromJsonElement(ListSerializer(ToolMetadata.serializer()), result)
                    results.addAll(filterTools(serverConfig.name, tools, query))
                } catch (e: Exception) {
                    logger.error(e) { "Error searching tools for ${serverConfig.name}" }
                }
            }
        }
        return results
    }

    fun filterTools(serverName: String, tools: List<ToolMetadata>, query: String): List<SearchResult> {
        return tools.filter { tool ->
            tool.name.contains(query, ignoreCase = true) ||
                    tool.description?.contains(query, ignoreCase = true) == true
        }.map { tool ->
            val descMatch = tool.description?.indexOf(query, ignoreCase = true) ?: -1
            val preview = if (descMatch >= 0) {
                tool.description?.substring(
                    max(0, descMatch - 30),
                    min(tool.description.length, descMatch + query.length + 30)
                ) ?: ""
            } else {
                ""
            }
            SearchResult(serverName, tool.name, tool.description, preview)
        }
    }
}
