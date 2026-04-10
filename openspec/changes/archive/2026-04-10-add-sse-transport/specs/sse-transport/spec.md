## ADDED Requirements

### Requirement: SSE transport configuration
The system SHALL support configuring MCP servers to use Server-Sent Events (SSE) transport via the `transport` field in server configuration.

#### Scenario: Configure server with SSE transport
- **WHEN** user sets `transport: "sse"` in server configuration
- **THEN** system SHALL use SSE transport for that server's connections

#### Scenario: Default to HTTP transport when not specified
- **WHEN** user does not specify `transport` field in server configuration
- **THEN** system SHALL default to HTTP POST transport for backward compatibility

#### Scenario: Reject invalid transport type
- **WHEN** user specifies an unsupported transport type
- **THEN** system SHALL reject the configuration with a clear error message

### Requirement: SSE connection establishment
The system SHALL establish persistent SSE connections to MCP servers configured with SSE transport when daemon starts.

#### Scenario: Successful SSE connection on daemon start
- **WHEN** daemon starts for a server with SSE transport configured
- **THEN** system SHALL establish an SSE connection to the server's endpoint
- **THEN** connection SHALL remain open to receive server events

#### Scenario: SSE connection with authentication
- **WHEN** daemon connects to SSE endpoint with auth configuration
- **THEN** system SHALL include appropriate authentication headers (Basic or Bearer)

#### Scenario: SSE connection timeout
- **WHEN** SSE connection attempt exceeds configured timeout
- **THEN** system SHALL fail with timeout error
- **THEN** daemon SHALL attempt reconnection per reconnection policy

### Requirement: SSE event stream parsing
The system SHALL parse incoming SSE events according to the SSE specification and extract MCP protocol messages.

#### Scenario: Parse valid SSE event
- **WHEN** system receives properly formatted SSE event with data field
- **THEN** system SHALL extract the event data as MCP message payload

#### Scenario: Handle SSE comment lines
- **WHEN** system receives SSE comment lines (starting with ':')
- **THEN** system SHALL ignore comment lines and continue processing

#### Scenario: Handle multi-line SSE data
- **WHEN** SSE event contains multiple data lines
- **THEN** system SHALL concatenate lines to form complete message payload

#### Scenario: Handle malformed SSE event
- **WHEN** system receives malformed SSE event
- **THEN** system SHALL log error and continue processing subsequent events without terminating connection

### Requirement: SSE automatic reconnection
The system SHALL automatically reconnect SSE streams when connection is lost, using exponential backoff strategy.

#### Scenario: Reconnect after connection drop
- **WHEN** active SSE connection is interrupted or closed
- **THEN** system SHALL attempt to reconnect automatically
- **THEN** first retry SHALL occur after 1 second delay

#### Scenario: Exponential backoff on repeated failures
- **WHEN** reconnection attempts fail consecutively
- **THEN** system SHALL increase delay between attempts exponentially (e.g., 1s, 2s, 4s, 8s)
- **THEN** delay SHALL be capped at a maximum value (e.g., 30 seconds)

#### Scenario: Maximum reconnection attempts
- **WHEN** reconnection attempts exceed configured maximum count
- **THEN** system SHALL stop reconnection attempts
- **THEN** daemon SHALL transition to failed state requiring manual restart

#### Scenario: Successful reconnection
- **WHEN** reconnection attempt succeeds after previous failures
- **THEN** system SHALL reset backoff delay to initial value
- **THEN** daemon SHALL resume normal operation

### Requirement: SSE request-response correlation
The system SHALL correlate MCP responses received via SSE stream with originating client requests using JSON-RPC 2.0 request IDs.

#### Scenario: Match response to pending request
- **WHEN** SSE event contains MCP response with request ID
- **THEN** system SHALL locate pending client request with matching ID
- **THEN** system SHALL forward response to waiting client via HTTP

#### Scenario: Handle response without matching request
- **WHEN** SSE event contains response with unknown request ID
- **THEN** system SHALL log warning about orphaned response
- **THEN** system SHALL discard response and continue processing

#### Scenario: Request timeout while waiting for SSE response
- **WHEN** client request timeout expires before SSE response arrives
- **THEN** system SHALL return timeout error to client
- **THEN** pending request SHALL be removed from correlation map

### Requirement: SSE server-initiated events
The system SHALL handle server-initiated events received via SSE stream that are not responses to client requests.

#### Scenario: Receive server notification event
- **WHEN** SSE stream delivers event without request ID (server-initiated notification)
- **THEN** system SHALL log the notification
- **THEN** system SHALL make notification available for monitoring/debugging purposes

#### Scenario: Server-initiated events do not block client requests
- **WHEN** server sends notifications via SSE stream
- **THEN** system SHALL continue processing client requests without interference

### Requirement: SSE connection lifecycle management
The system SHALL manage SSE connection lifecycle in coordination with daemon process lifecycle.

#### Scenario: Close SSE connection on daemon stop
- **WHEN** daemon receives stop command
- **THEN** system SHALL gracefully close SSE connection before terminating daemon process

#### Scenario: SSE connection status reporting
- **WHEN** user queries daemon status
- **THEN** system SHALL report SSE connection state (connected, reconnecting, failed)
- **THEN** status SHALL include last connection error if applicable

### Requirement: SSE error handling
The system SHALL handle SSE-specific errors with appropriate error messages and recovery actions.

#### Scenario: Handle HTTP error on SSE endpoint
- **WHEN** SSE connection attempt receives 4xx or 5xx HTTP status
- **THEN** system SHALL log specific HTTP error
- **THEN** system SHALL apply reconnection policy based on error type (e.g., no retry for 401 Unauthorized)

#### Scenario: Handle network error during SSE stream
- **WHEN** network error occurs on active SSE connection
- **THEN** system SHALL detect connection loss
- **THEN** system SHALL trigger reconnection logic

#### Scenario: Distinguish temporary vs permanent failures
- **WHEN** SSE connection fails with 503 Service Unavailable
- **THEN** system SHALL retry with backoff (temporary failure)
- **WHEN** SSE connection fails with 404 Not Found
- **THEN** system SHALL log permanent failure and stop retries
