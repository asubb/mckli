package com.mckli.ipc

import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.SocketChannel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class UnixSocketClient(private val socketPath: String) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun sendRequest(
        request: IpcRequest,
        timeout: Duration = 60.seconds
    ): Result<IpcResponse> {
        return try {
            // Connect to Unix domain socket
            val address = UnixDomainSocketAddress.of(socketPath)
            val channel = SocketChannel.open(StandardProtocolFamily.UNIX)

            // Set timeout
            channel.socket().soTimeout = timeout.inWholeMilliseconds.toInt()

            try {
                channel.connect(address)
                val socket = channel.socket()

                // Send request
                val writer = BufferedWriter(OutputStreamWriter(socket.outputStream))
                val requestLine = json.encodeToString(IpcRequest.serializer(), request)
                writer.write(requestLine)
                writer.newLine()
                writer.flush()

                // Receive response
                val reader = BufferedReader(InputStreamReader(socket.inputStream))
                val responseLine = reader.readLine()
                    ?: return Result.failure(IpcException("No response from daemon"))

                val response = json.decodeFromString<IpcResponse>(responseLine)
                Result.success(response)
            } finally {
                channel.close()
            }
        } catch (e: java.net.ConnectException) {
            Result.failure(IpcException("Cannot connect to daemon (is it running?)", e))
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(IpcException("Request timeout", e))
        } catch (e: UnsupportedOperationException) {
            Result.failure(IpcException("Unix domain sockets not supported on this platform. Try using the native binary instead of JAR.", e))
        } catch (e: Exception) {
            Result.failure(IpcException("IPC error: ${e.javaClass.simpleName}: ${e.message}", e))
        }
    }
}

class IpcException(message: String, cause: Throwable? = null) : Exception(message, cause)
