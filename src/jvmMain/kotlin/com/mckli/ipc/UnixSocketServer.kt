package com.mckli.ipc

import com.mckli.http.ConnectionPool
import com.mckli.http.McpRequest
import com.mckli.tools.ToolCache
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

class UnixSocketServer(
    private val socketPath: String,
    private val connectionPool: ConnectionPool,
    private val toolCache: ToolCache
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverChannel: ServerSocketChannel? = null
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun start() {
        // Create Unix domain socket
        val address = UnixDomainSocketAddress.of(socketPath)
        serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
        serverChannel?.bind(address)

        println("IPC server listening on: $socketPath")
        logger.info { "IPC server listening on: $socketPath" }

        // Accept connections in background
        scope.launch {
            while (isActive) {
                try {
                    val clientChannel = serverChannel?.accept()
                    if (clientChannel != null) {
                        logger.debug { "Accepted new connection" }
                        launch {
                            handleClient(clientChannel)
                        }
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        System.err.println("Error accepting connection: ${e.message}")
                        logger.error(e) { "Error accepting connection: ${e.message}" }
                    }
                }
            }
        }
    }

    private suspend fun handleClient(clientChannel: SocketChannel) {
        try {
            // Read request
            val inStream = java.nio.channels.Channels.newInputStream(clientChannel)
            val reader = BufferedReader(InputStreamReader(inStream))
            val requestLine = reader.readLine()
            if (requestLine == null) {
                logger.debug { "Client closed connection without request" }
                clientChannel.close()
                return
            }
            val request = json.decodeFromString<IpcRequest>(requestLine)

            logger.debug { "Received IPC request: $request" }

            // Process request
            val response = try {
                processRequest(request)
            } catch (e: Exception) {
                logger.error(e) { "Error processing IPC request: ${e.message}" }
                IpcResponse.Error(
                    requestId = request.requestId,
                    error = e.message ?: "Internal error",
                    details = e.stackTraceToString()
                )
            }

            logger.debug { "Sending IPC response for request: ${request.requestId}" }

            // Send response
            val outStream = java.nio.channels.Channels.newOutputStream(clientChannel)
            val writer = BufferedWriter(OutputStreamWriter(outStream))
            val responseLine = json.encodeToString(IpcResponse.serializer(), response)
            writer.write(responseLine)
            writer.newLine()
            writer.flush()
            // writer.close() // Don't close writer here as it closes the channel prematurely if we want to be sure, but clientChannel.close() is below

            clientChannel.close()
            logger.debug { "Connection closed" }
        } catch (e: Exception) {
            System.err.println("Error handling client: ${e.message}")
            // e.printStackTrace() // Avoid flooding console in tests
            logger.error(e) { "Error handling client: ${e.message}" }
            try { clientChannel.close() } catch (ex: Exception) {}
        }
    }

    private suspend fun processRequest(request: IpcRequest): IpcResponse {
        return try {
            when (request) {
                is IpcRequest.McpRequest -> {
                    val mcpRequest = McpRequest(
                        id = request.requestId,
                        method = request.method,
                        params = request.params
                    )

                    connectionPool.executeRequest { transport ->
                        transport.sendRequest(mcpRequest).fold(
                            onSuccess = { response ->
                                IpcResponse.Success(
                                    requestId = request.requestId,
                                    result = response.result ?: JsonNull
                                )
                            },
                            onFailure = { error ->
                                IpcResponse.Error(
                                    requestId = request.requestId,
                                    error = error.message ?: "Unknown error",
                                    details = error.stackTraceToString()
                                )
                            }
                        )
                    }
                }

                is IpcRequest.ListTools -> {
                    if (toolCache.getToolCount() == 0) {
                        logger.debug { "Tool cache empty, refreshing..." }
                        toolCache.refresh()
                    }
                    val tools = toolCache.listTools(request.filter)
                    IpcResponse.Success(
                        requestId = request.requestId,
                        result = Json.encodeToJsonElement(tools)
                    )
                }

                is IpcRequest.DescribeTool -> {
                    if (toolCache.getTool(request.toolName) == null) {
                        logger.debug { "Tool '${request.toolName}' not found in cache, refreshing..." }
                        toolCache.refresh()
                    }
                    val tool = toolCache.getTool(request.toolName)
                    if (tool != null) {
                        IpcResponse.Success(
                            requestId = request.requestId,
                            result = Json.encodeToJsonElement(tool)
                        )
                    } else {
                        IpcResponse.Error(
                            requestId = request.requestId,
                            error = "Tool '${request.toolName}' not found"
                        )
                    }
                }

                is IpcRequest.CallTool -> {
                    toolCache.callTool(request.toolName, request.arguments, connectionPool).fold(
                        onSuccess = { result ->
                            IpcResponse.Success(
                                requestId = request.requestId,
                                result = result
                            )
                        },
                        onFailure = { error ->
                            IpcResponse.Error(
                                requestId = request.requestId,
                                error = error.message ?: "Tool execution failed",
                                details = error.stackTraceToString()
                            )
                        }
                    )
                }

                is IpcRequest.RefreshTools -> {
                    toolCache.refresh()
                    IpcResponse.Success(
                        requestId = request.requestId,
                        result = JsonPrimitive("Tools refreshed successfully")
                    )
                }
            }
        } catch (e: Exception) {
            IpcResponse.Error(
                requestId = request.requestId,
                error = e.message ?: "Internal error",
                details = e.stackTraceToString()
            )
        }
    }

    fun stop() {
        scope.cancel()
        serverChannel?.close()
        Files.deleteIfExists(Paths.get(socketPath))
    }
}
