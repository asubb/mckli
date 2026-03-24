package com.mckli.http

import com.mckli.config.AuthConfig
import com.mckli.config.ServerConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json

class HttpMcpClient(private val config: ServerConfig) {
    private val client: HttpClient

    init {
        client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = false
                    ignoreUnknownKeys = true
                    isLenient = true
                })
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

    suspend fun sendRequest(request: McpRequest): Result<McpResponse> {
        return try {
            withTimeout(config.timeout) {
                val response: HttpResponse = client.post(config.endpoint) {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                when {
                    response.status.isSuccess() -> {
                        val mcpResponse = response.body<McpResponse>()
                        if (mcpResponse.error != null) {
                            Result.failure(McpException("MCP error: ${mcpResponse.error.message}", mcpResponse.error))
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

    fun close() {
        client.close()
    }
}

// Exception classes
sealed class McpClientException(message: String, cause: Throwable? = null) : Exception(message, cause)

class McpException(message: String, val error: McpError) : McpClientException(message)

class NetworkException(message: String, cause: Throwable? = null) : McpClientException(message, cause)

class TimeoutException(message: String) : McpClientException(message)

open class HttpException(val statusCode: Int, message: String) : McpClientException(message)

class ClientErrorException(statusCode: Int, message: String) : HttpException(statusCode, message)

class ServerErrorException(statusCode: Int, message: String) : HttpException(statusCode, message)
