package com.mckli.ipc

import com.mckli.http.ConnectionPool
import com.mckli.http.McpRequest
import com.mckli.tools.ToolCache
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
    }

    fun start() {
        // Create Unix domain socket
        val address = UnixDomainSocketAddress.of(socketPath)
        serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
        serverChannel?.bind(address)

        println("IPC server listening on: $socketPath")

        // Accept connections in background
        scope.launch {
            while (isActive) {
                try {
                    val clientChannel = serverChannel?.accept()
                    if (clientChannel != null) {
                        launch {
                            handleClient(clientChannel)
                        }
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        System.err.println("Error accepting connection: ${e.message}")
                    }
                }
            }
        }
    }

    private suspend fun handleClient(clientChannel: SocketChannel) {
        try {
            val socket = clientChannel.socket()
            val reader = BufferedReader(InputStreamReader(socket.inputStream))
            val writer = BufferedWriter(OutputStreamWriter(socket.outputStream))

            // Read request
            val requestLine = reader.readLine() ?: return
            val request = json.decodeFromString<IpcRequest>(requestLine)

            // Process request
            val response = processRequest(request)

            // Send response
            val responseLine = json.encodeToString(IpcResponse.serializer(), response)
            writer.write(responseLine)
            writer.newLine()
            writer.flush()

            clientChannel.close()
        } catch (e: Exception) {
            System.err.println("Error handling client: ${e.message}")
            e.printStackTrace()
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

                    connectionPool.executeRequest { client ->
                        client.sendRequest(mcpRequest).fold(
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
                    val tools = toolCache.listTools(request.filter)
                    IpcResponse.Success(
                        requestId = request.requestId,
                        result = Json.encodeToJsonElement(tools)
                    )
                }

                is IpcRequest.DescribeTool -> {
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
