## Context

The MCP CLI uses an HTTP POST-based transport layer with persistent daemon processes managing connections to MCP
servers. Current architecture:

- `ServerConfig` defines server endpoints and connection parameters
- `HttpMcpClient` handles HTTP POST requests with Ktor CIO engine
- `DaemonProcess` maintains long-lived connections via Unix sockets for IPC
- `RequestRouter` abstracts request routing between CLI and daemon

The MCP protocol specification defines multiple transport types, including SSE for streaming scenarios. Adding SSE
support requires extending the transport abstraction while preserving existing HTTP functionality.

## Goals / Non-Goals

**Goals:**

- Support SSE transport alongside HTTP POST without breaking existing configurations
- Enable automatic reconnection for SSE streams with configurable retry logic
- Handle SSE event parsing and message framing per MCP protocol
- Maintain daemon process model with SSE connection lifecycle management
- Provide clear configuration for transport type selection

**Non-Goals:**

- WebSocket transport (separate future work)
- Bidirectional streaming (SSE is server-to-client only; client requests use separate POST)
- Breaking changes to existing HTTP-only server configurations
- Native platform SSE support in this iteration (JVM only initially)

## Decisions

### 1. Transport Abstraction Layer

**Decision:** Create `McpTransport` interface with `HttpTransport` and `SseTransport` implementations

**Rationale:**

- Enables clean separation between HTTP POST and SSE logic
- Follows existing Kotlin Multiplatform expect/actual pattern for platform-specific code
- Allows future transport types (WebSocket) without refactoring core logic
- Factory pattern for transport selection based on `ServerConfig.transport` field

**Alternatives Considered:**

- Conditional logic in `HttpMcpClient`: Rejected due to mixing concerns and poor testability
- Separate client classes without interface: Rejected due to duplicate connection management code

### 2. SSE Implementation with Ktor

**Decision:** Use Ktor's SSE client support with CIO engine for JVM platform

**Rationale:**

- Ktor already used for HTTP client, consistent dependency strategy
- Built-in event parsing and connection management
- Supports timeouts, reconnection, and error handling out-of-box
- Multiplatform-ready (though initial implementation targets JVM only)

**Alternatives Considered:**

- Raw HTTP streaming with manual event parsing: Rejected due to complexity and reinventing wheel
- OkHttp SSE: Rejected to avoid mixing HTTP client libraries

### 3. Configuration Model Extension

**Decision:** Add optional `transport` field to `ServerConfig` with values `http` (default) or `sse`

**Rationale:**

- Backward compatible: existing configs without `transport` field default to HTTP
- Explicit transport selection prevents ambiguity
- Server endpoint URL doesn't always indicate transport type reliably

**Example:**

```kotlin
@Serializable
data class ServerConfig(
    val name: String,
    val endpoint: String,
    val transport: TransportType = TransportType.HTTP,
    val auth: AuthConfig? = null,
    val timeout: Long = 30000,
    val poolSize: Int = 10
)

@Serializable
enum class TransportType { HTTP, SSE }
```

### 4. SSE Connection Lifecycle in Daemon

**Decision:** Daemon process maintains persistent SSE connection with automatic reconnection on disconnect

**Rationale:**

- SSE streams are long-lived by design; daemon model fits naturally
- Reconnection logic centralized in daemon, not per-request
- IPC model unchanged: CLI sends requests via Unix socket, daemon routes via appropriate transport

**Flow:**

1. Daemon starts, establishes SSE connection to server
2. Connection monitors event stream for server-initiated messages
3. Client requests sent via POST to separate endpoint (SSE is server-to-client)
4. On disconnect, daemon attempts reconnection with exponential backoff

### 5. Event Handling and Message Correlation

**Decision:** SSE events carry MCP protocol messages; daemon correlates responses with request IDs

**Rationale:**

- MCP protocol uses JSON-RPC 2.0 with request/response ID correlation
- SSE stream delivers responses asynchronously
- Daemon maintains pending request map to match responses to waiting IPC clients

**Alternatives Considered:**

- Synchronous SSE read per request: Rejected; defeats SSE streaming purpose
- Webhook-based notifications: Rejected; requires external server setup

## Risks / Trade-offs

**[Risk: Native platform support delayed]** → Mitigation: Initial JVM-only implementation well-documented; Native
support added in follow-up when Ktor SSE Native stabilizes

**[Risk: Reconnection storms under server instability]** → Mitigation: Exponential backoff with max retry limit;
configurable reconnection policy in future iteration

**[Risk: Message ordering and loss during reconnection]** → Mitigation: Document SSE transport as best-effort; server
must handle idempotency; consider message queue for critical scenarios in future

**[Trade-off: SSE requires separate endpoint for client→server vs server→client]** → Accepted: MCP protocol over SSE
typically uses SSE for server push, HTTP POST for client requests. Document clearly in configuration.

**[Risk: Increased daemon complexity]** → Mitigation: Comprehensive integration tests covering SSE lifecycle; feature
flag for rollback if issues arise
