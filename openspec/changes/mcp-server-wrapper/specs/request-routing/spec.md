## ADDED Requirements

### Requirement: IPC connection establishment
The CLI SHALL establish IPC connection to daemon process using Unix domain sockets (or named pipes on Windows).

#### Scenario: Connect to running daemon
- **WHEN** CLI sends request to active daemon
- **THEN** system establishes socket connection to daemon IPC endpoint

#### Scenario: Daemon not running
- **WHEN** CLI attempts connection but daemon is not running
- **THEN** system detects connection failure and triggers auto-start

#### Scenario: Connection timeout
- **WHEN** socket connection attempt exceeds 5 second timeout
- **THEN** system returns connection timeout error

### Requirement: Request serialization
The CLI SHALL serialize MCP requests to JSON and send via IPC to appropriate daemon.

#### Scenario: Serialize MCP request
- **WHEN** CLI prepares request for daemon
- **THEN** system serializes request to JSON with request ID and payload

#### Scenario: Invalid request data
- **WHEN** request contains non-serializable data
- **THEN** system returns serialization error without sending

### Requirement: Server selection
The CLI SHALL route requests to the correct daemon based on target MCP server name.

#### Scenario: Route to named server
- **WHEN** user specifies server name in CLI command
- **THEN** system routes request to daemon for that server

#### Scenario: Default server
- **WHEN** user does not specify server name and default is configured
- **THEN** system routes request to default server daemon

#### Scenario: Unknown server
- **WHEN** user specifies server name not in configuration
- **THEN** system returns error listing available servers

### Requirement: Response handling
The CLI SHALL receive and deserialize responses from daemon via IPC.

#### Scenario: Successful response
- **WHEN** daemon returns successful MCP response
- **THEN** CLI deserializes JSON response and returns to user

#### Scenario: Error response
- **WHEN** daemon returns error response
- **THEN** CLI deserializes error and displays user-friendly message

#### Scenario: Response timeout
- **WHEN** daemon does not respond within configured timeout (default 60 seconds)
- **THEN** CLI cancels request and returns timeout error

### Requirement: Concurrent request handling
Daemons SHALL handle multiple concurrent requests from different CLI invocations.

#### Scenario: Multiple CLI clients
- **WHEN** multiple CLI processes send requests to same daemon
- **THEN** daemon accepts all connections and processes requests concurrently

#### Scenario: Request isolation
- **WHEN** daemon handles multiple concurrent requests
- **THEN** each request is isolated and does not affect others

### Requirement: Request correlation
The system SHALL correlate CLI requests with daemon responses using unique request IDs.

#### Scenario: Request ID assignment
- **WHEN** CLI sends request to daemon
- **THEN** system assigns unique request ID and includes in message

#### Scenario: Response matching
- **WHEN** daemon returns response
- **THEN** CLI matches response to original request using request ID

#### Scenario: Out-of-order responses
- **WHEN** daemon sends responses in different order than requests
- **THEN** CLI correctly matches each response to its request

### Requirement: Error propagation
The system SHALL propagate errors from MCP server through daemon to CLI with context.

#### Scenario: MCP server error
- **WHEN** remote MCP server returns error response
- **THEN** daemon forwards error to CLI with server name and error details

#### Scenario: Network error
- **WHEN** daemon encounters network error contacting MCP server
- **THEN** daemon returns error to CLI with connection details

#### Scenario: Daemon internal error
- **WHEN** daemon encounters internal error processing request
- **THEN** daemon returns error to CLI without crashing daemon process
