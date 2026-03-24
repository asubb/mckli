package com.mckli.client

import com.mckli.config.ConfigManager
import com.mckli.daemon.DaemonProcess
import com.mckli.ipc.IpcRequest
import com.mckli.ipc.IpcResponse
import com.mckli.ipc.UnixSocketClient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

actual class RequestRouter actual constructor(private val serverName: String?) {
    private val configManager = ConfigManager()

    actual fun sendMcpRequest(method: String, params: JsonElement?): Result<JsonElement> {
        return executeRequest { socketPath, requestId ->
            IpcRequest.McpRequest(requestId, method, params)
        }
    }

    actual fun listTools(filter: String?): Result<JsonElement> {
        return executeRequest { _, requestId ->
            IpcRequest.ListTools(requestId, filter)
        }
    }

    actual fun describeTool(toolName: String): Result<JsonElement> {
        return executeRequest { _, requestId ->
            IpcRequest.DescribeTool(requestId, toolName)
        }
    }

    actual fun callTool(toolName: String, arguments: JsonElement?): Result<JsonElement> {
        return executeRequest { _, requestId ->
            IpcRequest.CallTool(requestId, toolName, arguments)
        }
    }

    actual fun refreshTools(): Result<String> {
        return executeRequest<JsonElement> { _, requestId ->
            IpcRequest.RefreshTools(requestId)
        }.map { it.toString() }
    }

    private fun <T : JsonElement> executeRequest(
        requestBuilder: (String, String) -> IpcRequest
    ): Result<T> {
        return try {
            val serverConfig = getServerConfig(serverName, configManager)
            val daemon = DaemonProcess(serverConfig)

            // Auto-start daemon if not running
            if (!daemon.isRunning()) {
                daemon.start().onFailure { error ->
                    return Result.failure(RouterException("Failed to auto-start daemon: ${error.message}", error))
                }

                // Give daemon time to initialize
                Thread.sleep(1000)
            }

            val socketPath = daemon.getSocketPath()
            val requestId = generateRequestId()

            val request = requestBuilder(socketPath, requestId)
            val client = UnixSocketClient(socketPath)

            client.sendRequest(request).fold(
                onSuccess = { response ->
                    when (response) {
                        is IpcResponse.Success -> Result.success(response.result as T)
                        is IpcResponse.Error -> Result.failure(
                            RouterException("Request failed: ${response.error}\n${response.details ?: ""}")
                        )
                    }
                },
                onFailure = { error ->
                    Result.failure(RouterException("IPC failed: ${error.message}", error))
                }
            )
        } catch (e: Exception) {
            Result.failure(RouterException("Router error: ${e.message}", e))
        }
    }
}
