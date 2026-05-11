## Context

`mckli` currently uses a custom-built MCP transport and client layer. While functional, it requires manual updates for every change in the MCP specification. The official MCP Kotlin SDK (`org.modelcontextprotocol:kotlin-sdk`) provides a standardized, spec-compliant implementation of transports (SSE, Stdio) and the client protocol.

## Goals / Non-Goals

**Goals:**
- Replace custom `McpTransport`, `SseTransport`, and `HttpTransport` with SDK implementations.
- Use `McpClient` from the SDK for all MCP interactions (tool listing, calling tools).
- Maintain existing CLI commands and daemon functionality.
- Simplify the codebase by removing redundant transport logic.

**Non-Goals:**
- Adding support for new MCP features (e.g., resources, prompts) in this phase.
- Changing the HTTP API between CLI and Daemon.
- Switching from Netty (Ktor Server) to another server implementation.

## Decisions

### Decision: Use MCP Kotlin SDK v0.11.1
**Rationale:** The official Kotlin SDK reached maturity with version 0.11.x, providing a standardized implementation that reduces maintenance for `mckli`. Using the latest release ensures we have the most up-to-date protocol features and bug fixes.
**Alternatives Considered:** Staying with custom implementation (rejected to reduce maintenance burden).

### Decision: Use SDK's `SseClientTransport` and `HttpClientTransport`
**Rationale:** The SDK provides specialized transports for both SSE and HTTP (streamable). `mckli` will use these to replace its custom `SseTransport` and `HttpTransport`.
**Alternatives Considered:** Generic `ClientTransport` wrapper around existing Ktor client (rejected as SDK provides optimized implementations).

### Decision: Wrap `McpClient` in `UnifiedDaemon`
**Rationale:** `UnifiedDaemon` currently manages server connections. Each server connection should hold an instance of `McpClient` initialized with the appropriate transport.
**Alternatives Considered:** Global client (rejected, as each server needs its own transport/session).

### Decision: Migrate to SDK Models (`io.modelcontextprotocol.kotlin.sdk.types.*`)
**Rationale:** Standardizing on the SDK's types for requests (`CallToolRequest`), results (`CallToolResult`, `ListToolsResult`), and primitives (`Tool`, `Resource`) avoids mapping layers and ensures full spec compliance.
**Alternatives Considered:** Keeping `com.mckli.http.McpRequest` (rejected, as it's redundant with the SDK's request types).

## Risks / Trade-offs

- **[Risk] SDK Maturity** → The Kotlin SDK is relatively new. Mitigation: Verify core functionality (SSE, Tool calls) with existing integration tests.
- **[Risk] Breaking Changes in SDK** → Future SDK updates might break `mckli`. Mitigation: Pin SDK version in `build.gradle.kts`.
- **[Risk] Native Support** → MCP Kotlin SDK might have JVM-specific dependencies. Mitigation: `mckli` recently moved to JVM-only focus, so this is less of a concern now.
