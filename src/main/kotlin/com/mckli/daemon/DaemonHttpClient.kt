package com.mckli.daemon

import com.mckli.tools.ToolList
import com.mckli.tools.ToolMetadata
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

private val logger = KotlinLogging.logger {}

class DaemonHttpClient(private val baseUrl: String = "http://localhost:5030") {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60000L
            connectTimeoutMillis = 5000L
        }
    }

    suspend fun getHealth(serverName: String? = null): Result<DaemonStatus> {
        return try {
            val response = client.get("$baseUrl/health") {
                serverName?.let { parameter("server", it) }
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val errorBody = try { response.bodyAsText() } catch (e: Exception) { "" }
                Result.failure(Exception("Health check failed: ${response.status} $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listTools(serverName: String, filter: String? = null): Result<List<ToolMetadata>> {
        return try {
            val response = client.get("$baseUrl/servers/$serverName/tools") {
                filter?.let { parameter("filter", it) }
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<ToolList>().tools)
            } else {
                Result.failure(Exception("Failed to list tools: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun callTool(serverName: String, toolName: String, arguments: JsonElement?): Result<JsonElement> {
        return try {
            val response = client.post("$baseUrl/servers/$serverName/tools/call") {
                contentType(ContentType.Application.Json)
                setBody(ToolCallRequest(toolName, arguments))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val errorBody = try { response.bodyAsText() } catch (e: Exception) { "" }
                Result.failure(Exception("Tool call failed: ${response.status} $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshTools(serverName: String): Result<String> {
        return try {
            val response = client.post("$baseUrl/servers/$serverName/refresh")
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Refresh failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun close() {
        client.close()
    }
}
