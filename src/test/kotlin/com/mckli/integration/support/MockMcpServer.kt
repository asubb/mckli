package com.mckli.integration.support

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.uri
import io.ktor.server.response.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import java.util.concurrent.atomic.AtomicBoolean

class MockMcpServer(val port: Int = 8080) {
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private var mcpServer = createMcpServer()
    private val isResponding = AtomicBoolean(true)

    private fun createMcpServer() = Server(
        serverInfo = Implementation(
            name = "mock-server",
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
            intercept(ApplicationCallPipeline.Plugins) {
                if (!isResponding.get()) {
                    println("[DEBUG_LOG] MockMcpServer NOT responding - sending 503")
                    call.respond(HttpStatusCode.ServiceUnavailable, "Mock server set to not respond")
                    finish()
                }
            }
            mcpStreamableHttp(path = "/api") {
                println("[DEBUG_LOG] MockMcpServer responding to request")
                mcpServer
            }
        }
        server?.start(wait = false)
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
    }

    fun addTool(
        name: String,
        description: String? = null,
        inputSchema: JsonObject? = null,
        response: JsonElement? = null,
        error: String? = null,
        delayMs: Long = 0
    ) {
        println("[DEBUG_LOG] MockMcpServer adding tool: $name")
        mcpServer.addTool(
            name = name,
            description = description ?: "",
            inputSchema = ToolSchema(
                properties = inputSchema?.get("properties")?.jsonObject ?: buildJsonObject {},
                required = inputSchema?.get("required")?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            )
        ) { _ ->
            if (delayMs > 0) {
                delay(delayMs)
            }

            if (error != null) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(error)),
                    isError = true
                )
            }

            val content = when (val res = response) {
                is JsonObject -> {
                    if (res.containsKey("content") && res["content"] is JsonArray) {
                        res["content"]!!.jsonArray.map { contentJson ->
                            TextContent(contentJson.jsonObject["text"]?.jsonPrimitive?.content ?: contentJson.toString())
                        }
                    } else {
                        // Return the keys as part of a single text content if it's a flat object,
                        // to satisfy tests that expect certain keys in the result.
                        // If we return a TextContent with a JSON string, the daemon/SDK client
                        // might wrap it. 
                        // To satisfy `result[key1]` in ToolSteps, the result needs to be a JsonObject
                        // where key1 is a top-level property.
                        // SDK CallToolResult only allows content (list of Text/Image/Resource) and isError.
                        // However, we can return multiple TextContent items.
                        
                        // Return it as a JSON string in a single TextContent
                        listOf(TextContent(res.toString()))
                    }
                }
                null -> listOf(TextContent("Success"))
                else -> {
                    val str = res.toString().trim('"')
                    listOf(TextContent(str))
                }
            }
            
            // If the response is a JsonObject and we are in a mock server context, 
            // we might want to return it as a special response that the daemon can understand.
            // But the SDK is strict. 
            
            CallToolResult(
                content = content,
                isError = false
            )
        }
        
        // Notify clients that tools have changed
        // SDK requires a session ID, or we can use the notification to all if it supports it.
        // For now, let's just use runBlocking to ensure it's added.
    }

    fun clearTools() {
        mcpServer = createMcpServer()
    }

    fun setResponding(responding: Boolean) {
        println("[DEBUG_LOG] MockMcpServer setResponding: $responding")
        isResponding.set(responding)
    }

    fun reset() {
        clearTools()
        setResponding(true)
    }
}
