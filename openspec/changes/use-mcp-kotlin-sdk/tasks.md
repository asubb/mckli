## 1. Setup

- [x] 1.1 Add `io.modelcontextprotocol:kotlin-sdk:0.11.1` to `build.gradle.kts`.
- [x] 1.2 Add `io.ktor:ktor-client-sse` and verify other Ktor dependencies (must be 2.4.x or 3.0.x for better SDK compatibility, but we'll try with 2.3.12 first).

## 2. Transport Layer Migration

- [ ] 2.1 Refactor `TransportFactory` to return SDK's `ClientTransport` instead of `McpTransport`.
- [ ] 2.2 Migrate `SseTransport` implementation to use SDK's `SseClientTransport`.
- [ ] 2.3 Update `HttpTransport` to align with SDK's expected transport behavior.

## 3. Daemon Refactoring

- [ ] 3.1 Refactor `DaemonManager.ServerContext` to hold `io.modelcontextprotocol.kotlin.sdk.client.Client` instead of `McpTransport`.
- [ ] 3.2 Implement SDK's `connect()` lifecycle in `DaemonManager`.
- [ ] 3.3 Refactor `ToolCache` to use `client.listTools()` and handle the result as `ListToolsResult`.
- [ ] 3.4 Update `UnifiedDaemon` request routing: map incoming `ToolCallRequest` to `client.callTool()`.

## 4. CLI and Cleanup

- [ ] 4.1 Update `ToolCommands` to handle SDK-specific result types and errors.
- [ ] 4.2 Deprecate or remove `com.mckli.http.McpRequest`, `McpResponse` once migration is stable.
- [ ] 4.3 Remove custom SSE parsing logic from `com.mckli.transport.SseTransport`.

## 5. Verification

- [ ] 5.1 Run `SseTransportSteps` integration tests and update as needed for SDK behavior.
- [ ] 5.2 Run `ToolSteps` and `ToolInvocation` features to ensure E2E functionality.
- [ ] 5.3 Verify that `mckli tools list` and `mckli tools call` work correctly with the new SDK.
