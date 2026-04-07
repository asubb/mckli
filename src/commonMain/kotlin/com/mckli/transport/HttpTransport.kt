package com.mckli.transport

import com.mckli.config.AuthConfig
import com.mckli.config.ServerConfig
import com.mckli.http.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger {}

class HttpTransport(private val config: ServerConfig) : McpTransport {
    private val client: HttpClient
    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }
    private var isInitialized = false

    init {
        client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(json)
            }

            install(HttpTimeout) {
                requestTimeoutMillis = config.timeout
                connectTimeoutMillis = config.timeout
                socketTimeoutMillis = config.timeout
            }

            // Connection pooling configuration
            engine {
                maxConnectionsCount = config.poolSize
                endpoint {
                    maxConnectionsPerRoute = config.poolSize
                    keepAliveTime = 300000 // 5 minutes
                    connectTimeout = config.timeout
                    connectAttempts = 3
                }
            }

            defaultRequest {
                // Add authentication headers
                config.auth?.let { auth ->
                    when (auth) {
                        is AuthConfig.Basic -> {
                            basicAuth(auth.username, auth.password)
                        }

                        is AuthConfig.Bearer -> {
                            bearerAuth(auth.token)
                        }
                    }
                }
            }
        }
    }

    private suspend fun performInitialization(): Result<Unit> {
        if (isInitialized) return Result.success(Unit)

        logger.debug { "Initializing HTTP transport for ${config.name}" }
        val initRequest = McpRequest(
            id = "init-${Clock.System.now().toEpochMilliseconds()}",
            method = "initialize",
            params = json.encodeToJsonElement(
                McpInitializeParams.serializer(),
                McpInitializeParams(
                    protocolVersion = "2024-11-05",
                    clientInfo = McpClientInfo(name = "mckli-daemon", version = "1.0.0")
                )
            )
        )

        return sendRequest(initRequest).fold(
            onSuccess = { response ->
                isInitialized = true
                logger.debug { "HTTP transport initialized successfully for ${config.name}" }

                // Send initialized notification (optional for HTTP as it's stateless, but good for compliance)
                val notification = McpNotification(
                    method = "notifications/initialized",
                    params = null
                )
                // We don't wait for notification result
                @OptIn(DelicateCoroutinesApi::class)
                GlobalScope.launch {
                    try {
                        client.post(config.endpoint) {
                            contentType(ContentType.Application.Json)
                            setBody(notification)
                        }
                    } catch (e: Exception) {
                        // Ignore notification failures
                    }
                }

                Result.success(Unit)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    override suspend fun sendRequest(request: McpRequest): Result<McpResponse> {
        if (!isInitialized && request.method != "initialize") {
            val initResult = performInitialization()
            if (initResult.isFailure) {
                return Result.failure(initResult.exceptionOrNull()!!)
            }
        }

        return try {
            logger.debug { "Sending MCP request via HTTP to ${config.endpoint}: ${request.method} (id=${request.id})" }
            withTimeout(config.timeout) {
                val response: HttpResponse = client.post(config.endpoint) {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                logger.debug { "Received HTTP response: ${response.status} for ${request.method} (id=${request.id})" }

                when {
                    response.status.isSuccess() -> {
                        val bodyText = response.bodyAsText()
                        logger.debug { "Received HTTP response body: $bodyText" }
                        if (bodyText.isEmpty()) {
                            return@withTimeout Result.success(McpResponse())
                        }
                        val mcpResponse = try {
                            json.decodeFromString<McpResponse>(bodyText)
                        } catch (e: Exception) {
                            logger.error(e) { "Failed to decode MCP response: $bodyText" }
                            return@withTimeout Result.failure(
                                McpException(
                                    "Invalid MCP response: ${e.message}",
                                    McpError(-32603, "Internal error")
                                )
                            )
                        }

                        if (mcpResponse.error != null) {
                            logger.debug { "MCP response contains error: ${mcpResponse.error.message}" }
                            Result.failure(McpException(mcpResponse.error.message, mcpResponse.error))
                        } else if (mcpResponse.result == null && mcpResponse.id != null && request.method != "notifications/initialized") {
                            // If bodyText is "{}" or similar, decodeFromString might succeed with all-null fields
                            if (bodyText.contains("\"error\"") || bodyText.contains("'error'")) {
                                Result.failure(
                                    McpException(
                                        "Tool execution failed",
                                        McpError(-32603, "Tool execution failed")
                                    )
                                )
                            } else {
                                Result.success(mcpResponse)
                            }
                        } else {
                            Result.success(mcpResponse)
                        }
                    }

                    response.status.value in 400..499 -> {
                        val body = response.bodyAsText()
                        Result.failure(ClientErrorException(response.status.value, "Client error: $body"))
                    }

                    response.status.value in 500..599 -> {
                        val body = response.bodyAsText()
                        Result.failure(ServerErrorException(response.status.value, "Server error: $body"))
                    }

                    else -> {
                        Result.failure(HttpException(response.status.value, "HTTP error: ${response.status}"))
                    }
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Result.failure(TimeoutException("Request timeout after ${config.timeout}ms"))
        } catch (e: io.ktor.client.network.sockets.ConnectTimeoutException) {
            Result.failure(NetworkException("Connection timeout", e))
        } catch (e: io.ktor.client.network.sockets.SocketTimeoutException) {
            Result.failure(NetworkException("Socket timeout", e))
        } catch (e: java.net.UnknownHostException) {
            Result.failure(NetworkException("Unknown host: ${config.endpoint}", e))
        } catch (e: Exception) {
            Result.failure(NetworkException("Network error: ${e.message}", e))
        }
    }

    override fun close() {
        client.close()
    }
}
