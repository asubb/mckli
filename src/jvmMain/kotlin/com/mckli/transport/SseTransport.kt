package com.mckli.transport

import com.mckli.config.AuthConfig
import com.mckli.config.ServerConfig
import com.mckli.http.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class SseTransport(private val config: ServerConfig) : McpTransport {
    private val client: HttpClient
    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isConnected = AtomicBoolean(false)
    private val isClosed = AtomicBoolean(false)

    // Pending requests waiting for responses
    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<McpResponse>>()

    // SSE connection state
    private val _connectionState = MutableStateFlow<SseConnectionState>(SseConnectionState.Disconnected)
    override val connectionState: StateFlow<SseConnectionState> = _connectionState

    // Dynamic POST endpoint received from SSE
    private var postEndpoint = config.endpoint

    // Reconnection strategy
    private val reconnectionStrategy = ReconnectionStrategy()

    private var sseJob: Job? = null
    private var shouldReconnect = AtomicBoolean(true)

    private val isInitialized = AtomicBoolean(false)

    init {
        client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(json)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = null // Disable request timeout for SSE
                connectTimeoutMillis = config.timeout
                socketTimeoutMillis = Long.MAX_VALUE // Keep-alive handled by server/client
            }

            engine {
                maxConnectionsCount = config.poolSize
                endpoint {
                    maxConnectionsPerRoute = config.poolSize
                    keepAliveTime = 300000 // 5 minutes
                    connectTimeout = config.timeout
                    connectAttempts = 3
                }
            }
        }
    }

    /**
     * Establish SSE connection with automatic reconnection. Should be called before sending requests.
     */
    override suspend fun connect(): Result<Unit> {
        if (isConnected.get()) {
            return Result.success(Unit)
        }

        if (isClosed.get()) {
            return Result.failure(IllegalStateException("Transport is closed"))
        }

        return try {
            _connectionState.value = SseConnectionState.Connecting
            logger.debug { "Connecting to SSE endpoint: ${config.endpoint}" }

            sseJob = scope.launch {
                while (shouldReconnect.get() && !isClosed.get()) {
                    try {
                        _connectionState.value = SseConnectionState.Connecting

                        // Make HTTP request with Accept: text/event-stream
                        client.prepareGet(config.endpoint) {
                            header(HttpHeaders.Accept, "text/event-stream")
                            header(HttpHeaders.CacheControl, "no-cache")
                            header(HttpHeaders.Connection, "keep-alive")

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
                        }.execute { response ->
                            if (!response.status.isSuccess()) {
                                logger.error { "SSE connection failed with status: ${response.status}" }
                                throw Exception("SSE connection failed: ${response.status}")
                            }

                            isConnected.set(true)
                            isInitialized.set(false) // Reset initialization state on new connection
                            _connectionState.value = SseConnectionState.Connected
                            reconnectionStrategy.reset() // Successful connection, reset backoff
                            postEndpoint = config.endpoint // Reset to base endpoint on new connection
                            logger.info { "SSE connection established" }

                            // Read SSE stream
                            val channel = response.bodyAsChannel()
                            readSseStream(channel)
                        }
                    } catch (e: Exception) {
                        isConnected.set(false)
                        if (isClosed.get()) {
                            logger.debug { "SSE connection closed during read" }
                            return@launch
                        }

                        // Fail all pending requests
                        pendingRequests.values.forEach { deferred ->
                            deferred.completeExceptionally(NetworkException("SSE connection lost", e))
                        }
                        pendingRequests.clear()

                        // Attempt reconnection with exponential backoff
                        if (shouldReconnect.get() && !isClosed.get()) {
                            _connectionState.value = SseConnectionState.Reconnecting

                            val delayMs = reconnectionStrategy.getNextDelay()
                            if (delayMs == null) {
                                // Max retries exceeded
                                logger.error(e) { "Max SSE reconnection attempts exceeded" }
                                _connectionState.value = SseConnectionState.Failed("Max reconnection attempts exceeded")
                                shouldReconnect.set(false)
                                break
                            }

                            val currentAttempt = reconnectionStrategy.getAttemptCount() + 1
                            logger.warn(e) { "SSE connection lost. Reconnecting in ${delayMs}ms (attempt $currentAttempt)..." }
                            reconnectionStrategy.recordAttempt()
                        }
                    }
                }

                if (!isClosed.get() && _connectionState.value !is SseConnectionState.Failed) {
                    _connectionState.value = SseConnectionState.Disconnected
                }
            }

            // Wait for connection to be established (with timeout)
            withTimeout(config.timeout) {
                while (!isConnected.get() && _connectionState.value !is SseConnectionState.Failed) {
                    delay(100)
                }

                if (_connectionState.value is SseConnectionState.Failed) {
                    val state = _connectionState.value as SseConnectionState.Failed
                    throw Exception(state.error)
                }
            }

            Result.success(Unit)
        } catch (e: TimeoutCancellationException) {
            _connectionState.value = SseConnectionState.Failed("Connection timeout")
            Result.failure(TimeoutException("SSE connection timeout after ${config.timeout}ms"))
        } catch (e: Exception) {
            _connectionState.value = SseConnectionState.Failed(e.message ?: "Connection failed")
            Result.failure(NetworkException("Failed to establish SSE connection", e))
        }
    }

    private suspend fun readSseStream(channel: ByteReadChannel) {
        val buffer = StringBuilder()
        var currentEvent: String? = null

        try {
            while (!channel.isClosedForRead && !isClosed.get()) {
                val line = channel.readUTF8Line() ?: break
                logger.debug { "Read SSE stream line: $line" }

                // SSE format: each line is either empty (end of event) or field:value
                if (line.isEmpty()) {
                    // End of event, process accumulated data
                    if (buffer.isNotEmpty()) {
                        val eventData = buffer.toString().trim()
                        buffer.clear()

                        if (eventData.isNotEmpty()) {
                            handleSseEvent(currentEvent, eventData)
                        }
                    }
                    currentEvent = null // Reset event for next block
                } else if (line.startsWith(":")) {
                    // Comment line, ignore
                    continue
                } else if (line.startsWith("data:")) {
                    // Data line
                    val data = line.substring(5).trim()
                    if (buffer.isNotEmpty()) {
                        buffer.append("\n")
                    }
                    buffer.append(data)
                } else if (line.startsWith("event:")) {
                    // Event type
                    currentEvent = line.substring(6).trim()
                } else if (line.contains(":")) {
                    // Other field types (id, retry) - ignore for now
                    continue
                }
            }
        } catch (e: Exception) {
            if (!isClosed.get()) {
                throw e
            }
        }
    }

    private fun handleSseEvent(event: String?, data: String) {
        // Handle "endpoint" event specifically
        if (event == "endpoint") {
            try {
                // If it's a relative path, resolve it against the base URL
                val newEndpoint = if (data.startsWith("/")) {
                    val host = config.endpoint.substringBefore("://") + "://" + config.endpoint.substringAfter("://").substringBefore("/")
                    host + data
                } else {
                    data
                }
                logger.info { "Updated POST endpoint from SSE: $newEndpoint" }
                postEndpoint = newEndpoint
                return
            } catch (e: Exception) {
                logger.error(e) { "Failed to parse endpoint from SSE event: $data" }
            }
        }

        try {
            // Parse MCP response from event data
            val mcpResponse = try {
                json.decodeFromString<McpResponse>(data)
            } catch (e: Exception) {
                // Log malformed event but continue processing
                logger.warn(e) { "Malformed SSE event data: $data (event: $event)" }
                return
            }

            // Check if this is a response to a pending request
            val id = mcpResponse.id
            if (id != null) {
                val deferred = pendingRequests.remove(id)
                if (deferred != null) {
                    // Response to a client request
                    deferred.complete(mcpResponse)
                } else {
                    // Server-initiated event with ID (might be a request from server, not yet supported)
                    handleServerInitiatedEvent(mcpResponse)
                }
            } else {
                // Server-initiated notification (no ID)
                handleServerInitiatedEvent(mcpResponse)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error handling SSE event: ${e.message}" }
        }
    }

    private fun handleServerInitiatedEvent(response: McpResponse) {
        logger.debug { "Server notification received: id=${response.id}, method=${response.method ?: response.result}" }
        // TODO: Make available for monitoring/debugging
    }

    private suspend fun performInitialization(): Result<Unit> {
        if (isInitialized.get()) return Result.success(Unit)

        logger.info { "Performing MCP initialization handshake" }
        val initRequest = McpRequest(
            id = "init-${System.currentTimeMillis()}",
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
                logger.info { "MCP initialization successful" }
                isInitialized.set(true)

                // Send initialized notification
                scope.launch {
                    val notification = McpNotification(
                        method = "notifications/initialized",
                        params = null
                    )
                    sendPostRequest(notification)
                }

                Result.success(Unit)
            },
            onFailure = { error ->
                logger.error(error) { "MCP initialization failed" }
                Result.failure(error)
            }
        )
    }

    override suspend fun sendRequest(request: McpRequest): Result<McpResponse> {
        if (!isConnected.get()) {
            return Result.failure(IllegalStateException("SSE transport not connected. Call connect() first."))
        }

        if (isClosed.get()) {
            return Result.failure(IllegalStateException("Transport is closed"))
        }

        // Perform initialization if needed
        if (!isInitialized.get() && request.method != "initialize") {
            val initResult = performInitialization()
            if (initResult.isFailure) {
                return Result.failure(initResult.exceptionOrNull()!!)
            }
        }

        return try {
            // Create a deferred for this request
            val deferred = CompletableDeferred<McpResponse>()
            pendingRequests[request.id] = deferred

            // Send request via HTTP POST (SSE is for server-to-client only)
            // For MCP over SSE, client requests typically use a separate POST endpoint
            // and responses come back via the SSE stream
            val postResult = sendPostRequest(request)
            if (postResult.isFailure) {
                pendingRequests.remove(request.id)
                return Result.failure(postResult.exceptionOrNull()!!)
            }

            // Wait for response from SSE stream with timeout
            withTimeout(config.timeout) {
                try {
                    val response = deferred.await()

                    if (response.error != null) {
                        Result.failure(McpException("MCP error: ${response.error.message}", response.error))
                    } else {
                        Result.success(response)
                    }
                } catch (e: TimeoutCancellationException) {
                    pendingRequests.remove(request.id)
                    Result.failure(TimeoutException("Request timeout after ${config.timeout}ms"))
                }
            }
        } catch (e: TimeoutCancellationException) {
            pendingRequests.remove(request.id)
            Result.failure(TimeoutException("Request timeout after ${config.timeout}ms"))
        } catch (e: Exception) {
            pendingRequests.remove(request.id)
            Result.failure(NetworkException("Request failed", e))
        }
    }

    private suspend fun sendPostRequest(message: McpMessage): Result<Unit> {
        return try {
            val response = client.post(postEndpoint) {
                logger.debug { "Sending POST request to: $postEndpoint" }
                contentType(ContentType.Application.Json)
                setBody(message)

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

            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(HttpException(response.status.value, "HTTP error: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(NetworkException("Failed to send POST request", e))
        }
    }

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            // Cancel SSE connection
            sseJob?.cancel()

            // Fail all pending requests
            pendingRequests.values.forEach { deferred ->
                deferred.cancel()
            }
            pendingRequests.clear()

            // Close HTTP client
            client.close()

            // Cancel coroutine scope
            scope.cancel()

            isConnected.set(false)
            _connectionState.value = SseConnectionState.Disconnected
        }
    }
}

