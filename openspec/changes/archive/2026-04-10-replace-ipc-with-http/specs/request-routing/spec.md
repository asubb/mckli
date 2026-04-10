## MODIFIED Requirements

### Requirement: HTTP connection establishment
The CLI SHALL establish an HTTP connection to the global daemon process.

#### Scenario: Connect to running daemon
- **WHEN** the CLI sends a request to the active daemon
- **THEN** the system establishes an HTTP connection to the daemon's local endpoint (default http://localhost:5030)

#### Scenario: Daemon not running
- **WHEN** the CLI attempts a connection but the daemon is not running
- **THEN** the system detects the connection failure and triggers an auto-start

#### Scenario: Connection timeout
- **WHEN** the HTTP connection attempt exceeds a 5-second timeout
- **THEN** the system returns a connection timeout error

### Requirement: Request serialization
The CLI SHALL serialize MCP requests to JSON and send them via HTTP to the global daemon.

#### Scenario: Serialize MCP request
- **WHEN** the CLI prepares a request for the daemon
- **THEN** the system serializes the request to JSON with a request ID and payload

### Requirement: Server selection
The CLI SHALL route requests to the global daemon, specifying the target MCP server in the URL path.

#### Scenario: Route to named server
- **WHEN** the user specifies a server name in a CLI command
- **THEN** the system sends the HTTP request to `/servers/{serverName}/...` on the global daemon

#### Scenario: Default server
- **WHEN** the user does not specify a server name and a default is configured
- **THEN** the system routes the request to the default server's path on the global daemon

### Requirement: Response handling
The CLI SHALL receive and deserialize responses from the daemon via HTTP.

#### Scenario: Successful response
- **WHEN** the daemon returns a successful (200 OK) HTTP response with an MCP result
- **THEN** the CLI deserializes the JSON response and returns it to the user

#### Scenario: Error response
- **WHEN** the daemon returns an HTTP error status or an MCP error object
- **THEN** the CLI deserializes the error and displays a user-friendly message

### Requirement: Concurrent request handling
The global daemon SHALL handle multiple concurrent HTTP requests from different CLI invocations.

#### Scenario: Multiple CLI clients
- **WHEN** multiple CLI processes send HTTP requests to the same daemon
- **THEN** the daemon accepts all connections and processes the requests concurrently using coroutines

## REMOVED Requirements

### Requirement: IPC connection establishment
**Reason**: Replaced by HTTP communication.
**Migration**: Use the HTTP transport for CLI-to-daemon communication.
