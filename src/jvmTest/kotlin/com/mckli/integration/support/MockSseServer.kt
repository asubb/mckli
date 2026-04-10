package com.mckli.integration.support

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentHashMap

class MockSseServer(private val port: Int = 8081) {
    private var server: NettyApplicationEngine? = null
    private val isResponding = AtomicBoolean(true)
    private val sseChannels = ConcurrentHashMap<Int, Channel<String>>()
    private var channelIdCounter = 0
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val pendingRequests = ConcurrentHashMap<String, JsonObject>()
    private val initializedSessions = mutableSetOf<String>()
    private var sessionEndpoint: String? = null
    private val requestCount = ConcurrentHashMap<String, Int>()
    private val tools = mutableListOf<MockTool>()

    data class MockTool(
        val name: String,
        val description: String?,
        val inputSchema: JsonObject? = null
    )

    data class SseEvent(
        val id: String? = null,
        val event: String? = null,
        val data: String,
        val retry: Int? = null
    )

    fun setSessionEndpoint(endpoint: String?) {
        sessionEndpoint = endpoint
    }

    fun getRequestCount(path: String): Int = requestCount.getOrDefault(path, 0)

    fun start() {
        server = embeddedServer(Netty, port = port) {
            install(ContentNegotiation) {
                json()
            }
            routing {
                // SSE endpoint for receiving events
                get("/sse") {
                    if (!isResponding.get()) {
                        call.respond(HttpStatusCode.ServiceUnavailable)
                        return@get
                    }

                    val channelId = channelIdCounter++
                    val channel = Channel<String>(Channel.UNLIMITED)
                    sseChannels[channelId] = channel

                    try {
                        call.response.cacheControl(CacheControl.NoCache(null))
                        call.response.header(HttpHeaders.ContentType, "text/event-stream")
                        call.response.header(HttpHeaders.Connection, "keep-alive")
                        
                        // Start SSE stream by writing a comment or initial retry
                        call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                            write(": connected\n\n")
                            flush()

                            // If session endpoint is configured, send it immediately
                            sessionEndpoint?.let { endpoint ->
                                val event = SseEvent(event = "endpoint", data = endpoint)
                                val formatted = formatEvent(event)
                                write(formatted)
                                flush()
                            }
                            try {
                                while (true) {
                                    val event = withTimeout(30000) {
                                        channel.receive()
                                    }
                                    write(event)
                                    flush()
                                }
                            } catch (e: TimeoutCancellationException) {
                                // Connection timeout
                            } catch (e: Exception) {
                                // Client disconnected or other error
                            } finally {
                                sseChannels.remove(channelId)
                                channel.close()
                            }
                        }
                    } catch (e: Exception) {
                        sseChannels.remove(channelId)
                        channel.close()
                    }
                }

                // POST endpoint for receiving client requests
                post("/sse") {
                    requestCount["/sse"] = requestCount.getOrDefault("/sse", 0) + 1
                    handlePostRequest(call)
                }

                post("/messages/") {
                    requestCount["/messages/"] = requestCount.getOrDefault("/messages/", 0) + 1
                    handlePostRequest(call)
                }

                get("/health") {
                    call.respond(HttpStatusCode.OK, "OK")
                }
            }
        }.start(wait = false)
    }

    private suspend fun handlePostRequest(call: ApplicationCall) {
        if (!isResponding.get()) {
            call.respond(HttpStatusCode.ServiceUnavailable)
            return
        }

        try {
            val request = call.receive<JsonObject>()
            val id = request["id"]?.jsonPrimitive?.content ?: "unknown"
            val method = request["method"]?.jsonPrimitive?.content
            
            val sessionId = call.request.queryParameters["session_id"] ?: "default"

            if (method != "initialize" && method != "notifications/initialized" && !initializedSessions.contains(sessionId)) {
                call.respond(HttpStatusCode.BadRequest, "Request before initialization")
                return
            }

            // Store request for processing
            pendingRequests[id] = request

            // Send response via SSE
            scope.launch {
                delay(100) // Small delay to simulate processing

                val response = when (method) {
                    "tools/list" -> buildJsonObject {
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
                    "initialize" -> {
                        initializedSessions.add(sessionId)
                        buildJsonObject {
                            put("jsonrpc", "2.0")
                            put("id", id)
                            put("result", buildJsonObject {
                                put("protocolVersion", "2024-11-05")
                                put("capabilities", buildJsonObject {})
                                put("serverInfo", buildJsonObject {
                                    put("name", "mock-sse-server")
                                    put("version", "1.0.0")
                                })
                            })
                        }
                    }
                    "notifications/initialized" -> null
                    else -> buildJsonObject {
                        put("jsonrpc", "2.0")
                        put("id", id)
                        put("result", JsonNull)
                    }
                }

                if (response != null) {
                    sendEvent(SseEvent(
                        data = Json.encodeToString(response)
                    ))
                }
            }

            call.respond(HttpStatusCode.Accepted)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
        }
    }

    fun stop() {
        sseChannels.values.forEach { it.close() }
        sseChannels.clear()
        scope.cancel()
        server?.stop(1000, 2000)
        server = null
    }

    fun setResponding(responding: Boolean) {
        isResponding.set(responding)
    }

    fun addTool(name: String, description: String? = null, inputSchema: JsonObject? = null) {
        tools.add(MockTool(name, description, inputSchema))
    }

    fun clearTools() {
        tools.clear()
    }

    fun sendEvent(event: SseEvent) {
        val formatted = formatEvent(event)

        sseChannels.values.forEach { channel ->
            scope.launch {
                try {
                    channel.send(formatted)
                } catch (e: Exception) {
                    // Channel closed
                }
            }
        }
    }

    private fun formatEvent(event: SseEvent): String {
        return buildString {
            event.id?.let { appendLine("id: $it") }
            event.event?.let { appendLine("event: $it") }
            event.retry?.let { appendLine("retry: $it") }

            // Handle multi-line data
            event.data.lines().forEach { line ->
                appendLine("data: $line")
            }
            appendLine() // Empty line marks end of event
        }
    }

    fun disconnectAll() {
        sseChannels.values.forEach { it.close() }
        sseChannels.clear()
    }

    fun getActiveConnectionCount(): Int = sseChannels.size

    fun waitForConnection(timeoutMs: Long = 5000) {
        val startTime = System.currentTimeMillis()
        while (sseChannels.isEmpty() && (System.currentTimeMillis() - startTime) < timeoutMs) {
            Thread.sleep(100)
        }
    }
}
