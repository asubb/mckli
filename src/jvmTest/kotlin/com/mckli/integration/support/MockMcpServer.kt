package com.mckli.integration.support

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import java.util.concurrent.atomic.AtomicBoolean

class MockMcpServer(private val port: Int = 8080) {
    private var server: NettyApplicationEngine? = null
    private val tools = mutableListOf<MockTool>()
    private val isResponding = AtomicBoolean(true)

    data class MockTool(
        val name: String,
        val description: String?,
        val inputSchema: JsonObject?,
        val response: JsonElement? = null,
        val error: String? = null,
        val delayMs: Long = 0
    )

    fun start() {
        server = embeddedServer(Netty, port = port) {
            routing {
                post("/api") {
                    if (!isResponding.get()) {
                        call.respond(HttpStatusCode.ServiceUnavailable)
                        return@post
                    }

                    val request = call.receive<JsonObject>()
                    val method = request["method"]?.jsonPrimitive?.content
                    val id = request["id"]?.jsonPrimitive?.content ?: "unknown"

                    when (method) {
                        "tools/list" -> handleToolsList(id, call)
                        "tools/call" -> handleToolCall(id, request, call)
                        else -> call.respond(HttpStatusCode.NotFound, buildJsonObject {
                            put("jsonrpc", "2.0")
                            put("id", id)
                            put("error", buildJsonObject {
                                put("code", -32601)
                                put("message", "Method not found")
                            })
                        })
                    }
                }
            }
        }.start(wait = false)
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
        tools.add(MockTool(name, description, inputSchema, response, error, delayMs))
    }

    fun clearTools() {
        tools.clear()
    }

    fun setResponding(responding: Boolean) {
        isResponding.set(responding)
    }

    private suspend fun handleToolsList(id: String, call: ApplicationCall) {
        val toolList = buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", id)
            put("result", buildJsonObject {
                put("tools", buildJsonArray {
                    tools.forEach { tool ->
                        add(buildJsonObject {
                            put("name", tool.name)
                            tool.description?.let { put("description", it) }
                            tool.inputSchema?.let { put("inputSchema", it) }
                        })
                    }
                })
            })
        }
        call.respond(HttpStatusCode.OK, toolList)
    }

    private suspend fun handleToolCall(id: String, request: JsonObject, call: ApplicationCall) {
        val params = request["params"]?.jsonObject
        val toolName = params?.get("name")?.jsonPrimitive?.content

        val tool = tools.find { it.name == toolName }

        if (tool == null) {
            call.respond(HttpStatusCode.OK, buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", id)
                put("error", buildJsonObject {
                    put("code", -32602)
                    put("message", "Tool '$toolName' not found")
                })
            })
            return
        }

        // Simulate delay
        if (tool.delayMs > 0) {
            Thread.sleep(tool.delayMs)
        }

        if (tool.error != null) {
            call.respond(HttpStatusCode.OK, buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", id)
                put("error", buildJsonObject {
                    put("code", -32000)
                    put("message", tool.error)
                })
            })
        } else {
            call.respond(HttpStatusCode.OK, buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", id)
                put("result", tool.response ?: JsonNull)
            })
        }
    }
}
