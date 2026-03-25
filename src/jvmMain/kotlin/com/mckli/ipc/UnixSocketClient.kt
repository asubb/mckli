package com.mckli.ipc

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.Channels
import java.nio.channels.SocketChannel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

class UnixSocketClient(private val socketPath: String) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun sendRequest(
        request: IpcRequest,
        timeout: Duration = 60.seconds
    ): Result<IpcResponse> {
        logger.debug { "Sending IPC request to $socketPath: $request" }
        return try {
            // Connect to Unix domain socket
            val address = UnixDomainSocketAddress.of(socketPath)
            val channel = SocketChannel.open(StandardProtocolFamily.UNIX)

            // Set timeout
            // channel.socket().soTimeout is not supported for Unix domain sockets in some JVMs
            // We will rely on blocking I/O for now, or could use Selector for true timeout.
            // But let's at least remove the failing call.

            try {
                channel.connect(address)
                logger.debug { "Connected to IPC socket at $socketPath" }

                // Send request
                val outStream = Channels.newOutputStream(channel)
                val writer = BufferedWriter(OutputStreamWriter(outStream))
                val requestLine = json.encodeToString(IpcRequest.serializer(), request)
                writer.write(requestLine)
                writer.newLine()
                writer.flush()
                logger.debug { "IPC request sent" }

                // Receive response
                val inStream = Channels.newInputStream(channel)
                val reader = BufferedReader(InputStreamReader(inStream))
                val responseLine = reader.readLine()
                    ?: run {
                        logger.debug { "No response received from IPC socket" }
                        return Result.failure(IpcException("No response from daemon"))
                    }

                val response = json.decodeFromString<IpcResponse>(responseLine)
                logger.debug { "Received IPC response: $response" }
                Result.success(response)
            } finally {
                channel.close()
            }
        } catch (e: java.net.ConnectException) {
            logger.debug(e) { "Connection refused to IPC socket at $socketPath" }
            Result.failure(IpcException("Cannot connect to daemon (is it running?)", e))
        } catch (e: java.net.SocketTimeoutException) {
            logger.debug(e) { "Timeout waiting for IPC response from $socketPath" }
            Result.failure(IpcException("Request timeout", e))
        } catch (e: UnsupportedOperationException) {
            logger.error(e) { "Unix domain sockets not supported" }
            Result.failure(IpcException("Unix domain sockets not supported on this platform. Try using the native binary instead of JAR.", e))
        } catch (e: Exception) {
            logger.error(e) { "Unexpected IPC error: ${e.message}" }
            Result.failure(IpcException("IPC error: ${e.javaClass.simpleName}: ${e.message}", e))
        }
    }
}

class IpcException(message: String, cause: Throwable? = null) : Exception(message, cause)
