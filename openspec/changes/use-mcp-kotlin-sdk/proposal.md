## Why

Currently, `mckli` implements its own Model Context Protocol (MCP) transport and client logic. Using the official `kotlin-sdk` from the Model Context Protocol organization ensures better compatibility with the evolving MCP specification, reduces maintenance burden, and avoids "reinventing the wheel."

## What Changes

- Add `org.modelcontextprotocol:kotlin-sdk` dependency to the project.
- Replace custom `McpTransport`, `HttpTransport`, and `SseTransport` implementations with SDK-provided transports.
- Refactor `UnifiedDaemon` and related components to use the official SDK client for communicating with MCP servers.
- **BREAKING**: Custom transport interfaces and request/response models (`McpRequest`, `McpResponse`) will be replaced by SDK equivalents.

## Capabilities

### New Capabilities
- `sdk-integration`: Integration of the official MCP Kotlin SDK as the core communication layer.

### Modified Capabilities
- `sse-transport`: Switch from custom SSE implementation to the SDK's SSE transport.
- `mcp-tool-execution`: Use the SDK's client to invoke tools on MCP servers.
- `mcp-tool-discovery`: Use the SDK's client for listing and searching tools.

## Impact

- `build.gradle.kts`: New dependency added.
- `com.mckli.transport`: Package will be heavily refactored or deprecated in favor of SDK transports.
- `com.mckli.http`: Custom request/response models will be replaced.
- `com.mckli.daemon`: `UnifiedDaemon` will use SDK client for routing requests.
- Integration tests: Will need updates to match SDK's API and behavior.
