package com.mckli.integration.support

import io.ktor.http.HttpStatusCode
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import java.util.concurrent.atomic.AtomicBoolean

class MockSseServer(val port: Int = 8081) {
    private val isResponding = AtomicBoolean(true)
    private var mcpServer = createMcpServer()
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null

    private fun createMcpServer() = Server(
        serverInfo = Implementation(
            name = "mock-sse-server",
            version = "1.0.0"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true),
            ),
        )
    )

    fun start() {
        server = embeddedServer(Netty, port = port, host = "127.0.0.1") {
            install(SSE)
            routing {
                get("/health") {
                    call.respondText("OK")
                }
                mcp {
                    if (!isResponding.get()) {
                        error("Not responding")
                    } else {
                        mcpServer
                    }
                }
            }
        }
        server?.start(wait = false)
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
    }

    fun setResponding(responding: Boolean) {
        isResponding.set(responding)
    }

    fun addTool(
        name: String,
        description: String? = null,
        inputSchema: JsonObject? = null,
        response: JsonElement? = null,
        error: String? = null,
        delayMs: Long = 0
    ) {
        mcpServer.addTool(
            name = name,
            description = description ?: "",
            inputSchema = ToolSchema(
                properties = inputSchema?.get("properties")?.jsonObject ?: buildJsonObject {},
                required = inputSchema?.get("required")?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            )
        ) { request ->
            if (delayMs > 0) {
                delay(delayMs)
            }

            if (error != null) {
                throw Exception(error)
            }

            val content = when (val res = response) {
                is JsonObject -> {
                    if (res.containsKey("content") && res["content"] is JsonArray) {
                        res["content"]!!.jsonArray.map { contentJson ->
                            TextContent(contentJson.jsonObject["text"]?.jsonPrimitive?.content ?: contentJson.toString())
                        }
                    } else {
                        listOf(TextContent(res.toString()))
                    }
                }
                null -> listOf(TextContent("Success"))
                else -> listOf(TextContent(res.toString().trim('"')))
            }
            CallToolResult(content = content)
        }
    }

    fun clearTools() {
        mcpServer = createMcpServer()
    }

    fun reset() {
        clearTools()
        setResponding(true)
    }
    
    // Compatibility methods if still needed by some tests
    fun setSessionEndpoint(endpoint: String?) {
        // Not easily supported with mcp { } helper as it manages endpoints
    }

    fun getRequestCount(path: String): Int = 0 // Not tracked anymore

    fun waitForConnection(timeoutMs: Long = 5000) {
        // Wait logic might need rethinking
    }

    fun getActiveConnectionCount(): Int = 0 // Not tracked anymore

    fun disconnectAll() {
        // Not easily supported
    }

    fun sendEvent(event: String? = null, data: String) {
        // Not easily supported
    }
}
