package com.mckli.integration.support

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
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

    data class SseEvent(
        val id: String? = null,
        val event: String? = null,
        val data: String,
        val retry: Int? = null
    )

    fun start() {
        server = embeddedServer(Netty, port = port) {
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
                        call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                            // Keep connection alive and send events
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
                    if (!isResponding.get()) {
                        call.respond(HttpStatusCode.ServiceUnavailable)
                        return@post
                    }

                    try {
                        val request = call.receive<JsonObject>()
                        val id = request["id"]?.jsonPrimitive?.content ?: "unknown"
                        val method = request["method"]?.jsonPrimitive?.content

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
                                        put("tools", buildJsonArray {})
                                    })
                                }
                                "initialize" -> buildJsonObject {
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
                                else -> buildJsonObject {
                                    put("jsonrpc", "2.0")
                                    put("id", id)
                                    put("result", JsonNull)
                                }
                            }

                            sendEvent(SseEvent(
                                data = Json.encodeToString(response)
                            ))
                        }

                        call.respond(HttpStatusCode.Accepted)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid request")
                    }
                }
            }
        }.start(wait = false)
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

    fun sendEvent(event: SseEvent) {
        val formatted = buildString {
            event.id?.let { appendLine("id: $it") }
            event.event?.let { appendLine("event: $it") }
            event.retry?.let { appendLine("retry: $it") }

            // Handle multi-line data
            event.data.lines().forEach { line ->
                appendLine("data: $line")
            }
            appendLine() // Empty line marks end of event
        }

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
