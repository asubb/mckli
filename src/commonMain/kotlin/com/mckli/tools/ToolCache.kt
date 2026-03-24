package com.mckli.tools

import com.mckli.http.ConnectionPool
import com.mckli.http.McpRequest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

class ToolCache(private val connectionPool: ConnectionPool) {
    private val mutex = Mutex()
    private val tools = mutableMapOf<String, ToolMetadata>()

    suspend fun refresh() {
        val request = McpRequest(
            id = "tool-list-${System.currentTimeMillis()}",
            method = "tools/list",
            params = null
        )

        connectionPool.executeRequest { client ->
            client.sendRequest(request).fold(
                onSuccess = { response ->
                    response.result?.let { result ->
                        try {
                            val toolList = Json.decodeFromJsonElement<ToolList>(result)
                            mutex.withLock {
                                tools.clear()
                                toolList.tools.forEach { tool ->
                                    tools[tool.name] = tool
                                }
                            }
                        } catch (e: Exception) {
                            throw ToolCacheException("Failed to parse tool list: ${e.message}", e)
                        }
                    }
                },
                onFailure = { error ->
                    throw ToolCacheException("Failed to fetch tools: ${error.message}", error)
                }
            )
        }
    }

    suspend fun listTools(filter: String? = null): List<ToolMetadata> {
        return mutex.withLock {
            if (filter != null) {
                tools.values.filter { tool ->
                    tool.name.contains(filter, ignoreCase = true) ||
                            (tool.description?.contains(filter, ignoreCase = true) == true)
                }
            } else {
                tools.values.toList()
            }
        }
    }

    suspend fun getTool(name: String): ToolMetadata? {
        return mutex.withLock {
            tools[name]
        }
    }

    suspend fun getToolCount(): Int {
        return mutex.withLock {
            tools.size
        }
    }

    suspend fun callTool(
        toolName: String,
        arguments: JsonElement?,
        pool: ConnectionPool
    ): Result<JsonElement> {
        val tool = getTool(toolName)
            ?: return Result.failure(ToolCacheException("Tool '$toolName' not found"))

        val request = McpRequest(
            id = "tool-call-${System.currentTimeMillis()}",
            method = "tools/call",
            params = buildJsonObject {
                put("name", toolName)
                if (arguments != null) {
                    put("arguments", arguments)
                }
            }
        )

        return pool.executeRequest { client ->
            client.sendRequest(request).fold(
                onSuccess = { response ->
                    response.result?.let { Result.success(it) }
                        ?: Result.failure(ToolCacheException("No result from tool call"))
                },
                onFailure = { error ->
                    Result.failure(ToolCacheException("Tool call failed: ${error.message}", error))
                }
            )
        }
    }
}

class ToolCacheException(message: String, cause: Throwable? = null) : Exception(message, cause)
