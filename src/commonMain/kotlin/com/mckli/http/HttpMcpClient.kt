package com.mckli.http

import com.mckli.config.ServerConfig
import com.mckli.transport.HttpTransport

/**
 * Backward compatibility wrapper for HttpTransport.
 * New code should use HttpTransport directly via TransportFactory.
 */
class HttpMcpClient(config: ServerConfig) {
    private val transport = HttpTransport(config)

    suspend fun sendRequest(request: McpRequest): Result<McpResponse> {
        return transport.sendRequest(request)
    }

    fun close() {
        transport.close()
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
