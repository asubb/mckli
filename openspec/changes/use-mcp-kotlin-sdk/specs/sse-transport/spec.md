## MODIFIED Requirements

### Requirement: SSE connection establishment
The system SHALL establish persistent SSE connections to MCP servers configured with SSE transport using the official MCP Kotlin SDK's `SseClientTransport`.

#### Scenario: Successful SSE connection on daemon start
- **WHEN** daemon starts for a server with SSE transport configured
- **THEN** system SHALL establish an SSE connection using `SseClientTransport` to the server's endpoint
- **THEN** connection SHALL remain open to receive server events

#### Scenario: SSE connection with authentication
- **WHEN** daemon connects to SSE endpoint with auth configuration
- **THEN** system SHALL include appropriate authentication headers (Basic or Bearer) via SDK transport configuration

#### Scenario: SSE connection timeout
- **WHEN** SSE connection attempt exceeds configured timeout
- **THEN** system SHALL fail with timeout error as reported by SDK

### Requirement: SSE event stream parsing
The system SHALL delegate SSE event stream parsing to the official MCP Kotlin SDK's `SseClientTransport`.

#### Scenario: Parse valid SSE event
- **WHEN** SDK's transport receives properly formatted SSE event
- **THEN** SDK SHALL extract and provide the event data as MCP message payload to the `McpClient`

### Requirement: SSE automatic reconnection
The system SHALL manage SSE reconnection leveraging SDK transport capabilities and standard Kotlin Coroutines error handling.

#### Scenario: Reconnect after connection drop
- **WHEN** active SSE connection is interrupted or closed
- **THEN** system SHALL attempt to reconnect automatically using configured reconnection policy

### Requirement: SSE request-response correlation
The system SHALL delegate MCP request-response correlation to the official MCP Kotlin SDK's `McpClient`.

#### Scenario: Match response to pending request
- **WHEN** MCP response is received via SDK transport
- **THEN** SDK's `McpClient` SHALL correlate it with the pending request and complete the request's deferred result

### Requirement: SSE server-initiated events
The system SHALL handle server-initiated notifications received via SDK's `McpClient` event stream.

#### Scenario: Receive server notification event
- **WHEN** SDK's `McpClient` receives a server notification
- **THEN** system SHALL log the notification or trigger appropriate local handlers
