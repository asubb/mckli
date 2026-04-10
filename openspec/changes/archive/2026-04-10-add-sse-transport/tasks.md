## 1. Configuration Model Extension

- [x] 1.1 Add `TransportType` enum to `ServerConfig.kt` with HTTP and SSE values
- [x] 1.2 Add `transport` field to `ServerConfig` data class with default value HTTP
- [x] 1.3 Update configuration validation to reject invalid transport types
- [x] 1.4 Add unit tests for ServerConfig with transport field variations

## 2. Transport Abstraction Layer

- [x] 2.1 Create `McpTransport` interface in `com.mckli.transport` package with sendRequest method
- [x] 2.2 Refactor `HttpMcpClient` to implement `McpTransport` as `HttpTransport`
- [x] 2.3 Create `TransportFactory` to instantiate appropriate transport based on ServerConfig
- [x] 2.4 Add unit tests for TransportFactory transport selection logic

## 3. SSE Client Implementation

- [x] 3.1 Add Ktor SSE client dependency to build.gradle.kts for JVM target
- [x] 3.2 Create `SseTransport` class implementing `McpTransport` interface
- [x] 3.3 Implement SSE connection establishment with authentication header support
- [x] 3.4 Implement SSE event stream parsing to extract MCP message payloads
- [x] 3.5 Add handling for SSE comment lines and multi-line data fields
- [x] 3.6 Implement error handling for malformed SSE events with logging

## 4. SSE Reconnection Logic

- [x] 4.1 Create `ReconnectionStrategy` class with exponential backoff implementation
- [x] 4.2 Implement reconnection trigger on SSE connection drop detection
- [x] 4.3 Add configurable maximum retry count and maximum backoff delay
- [x] 4.4 Implement backoff reset on successful reconnection
- [x] 4.5 Add unit tests for ReconnectionStrategy with various failure scenarios

## 5. Daemon SSE Integration

- [x] 5.1 Update `DaemonProcess` to initialize SSE transport when configured
- [x] 5.2 Implement persistent SSE connection lifecycle in daemon startup
- [x] 5.3 Add request-response correlation map using JSON-RPC request IDs
- [x] 5.4 Implement SSE event listener to forward responses to IPC clients
- [x] 5.5 Add handling for request timeout with correlation map cleanup
- [x] 5.6 Implement server-initiated event logging and monitoring
- [x] 5.7 Add graceful SSE connection closure on daemon stop command

## 6. Daemon Status Reporting

- [x] 6.1 Extend `DaemonStatus` data class to include SSE connection state
- [x] 6.2 Add fields for connection status (connected, reconnecting, failed)
- [x] 6.3 Add optional field for last connection error message
- [x] 6.4 Update daemon status command output to display SSE connection info

## 7. Request Router Updates

- [x] 7.1 Update `RequestRouter` to use `TransportFactory` for transport instantiation
- [x] 7.2 Ensure IPC request routing works with both HTTP and SSE transports
- [x] 7.3 Add error handling for transport-specific exceptions
- [x] 7.4 Add unit tests for RequestRouter with SSE transport

## 8. Error Handling and Recovery

- [x] 8.1 Implement HTTP error code handling for SSE endpoint (4xx, 5xx)
- [x] 8.2 Add retry policy logic for temporary failures (503) vs permanent (404, 401)
- [x] 8.3 Implement network error detection during active SSE stream
- [x] 8.4 Add timeout handling for SSE connection establishment
- [x] 8.5 Ensure orphaned response handling logs warnings appropriately

## 9. Integration Testing

- [x] 9.1 Create mock SSE server for testing in `MockMcpServer`
- [x] 9.2 Add Cucumber feature file for SSE transport scenarios
- [x] 9.3 Implement step definitions for SSE connection establishment
- [x] 9.4 Add scenarios for SSE reconnection with connection drops
- [x] 9.5 Add scenarios for request-response correlation via SSE
- [x] 9.6 Add scenarios for daemon status reporting with SSE state
- [x] 9.7 Add scenarios for SSE error handling and recovery

## 10. Documentation and Configuration Examples

- [x] 10.1 Document transport field in server configuration format
- [x] 10.2 Add example configuration with SSE transport in README
- [x] 10.3 Document SSE-specific behavior (server-to-client push, reconnection)
- [x] 10.4 Add troubleshooting guide for SSE connection issues
