package com.mckli.transport

import com.mckli.config.ServerConfig
import com.mckli.config.TransportType
import io.ktor.client.request.*
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.shared.Transport
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object TransportFactory {
    fun create(config: ServerConfig): Transport {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
            install(HttpTimeout) {
                connectTimeoutMillis = config.timeout
            }
        }

        return when (config.transport) {
            TransportType.HTTP -> StreamableHttpClientTransport(
                client = client,
                url = config.endpoint
            )
            TransportType.SSE -> {
                val sseClient = HttpClient(CIO) {
                    install(SSE)
                    install(HttpTimeout) {
                        connectTimeoutMillis = config.timeout
                    }
                    install(ContentNegotiation) {
                        json(Json {
                            ignoreUnknownKeys = true
                        })
                    }
                }
                SseClientTransport(
                    client = sseClient,
                    urlString = config.endpoint
                )
            }
        }
    }
}
