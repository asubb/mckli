## ADDED Requirements

### Requirement: Connection pool initialization
The unified daemon SHALL maintain a connection pool for each configured MCP server instance.

#### Scenario: Pool created on daemon start
- **WHEN** daemon process starts or a new server is added
- **THEN** a connection pool for each server is initialized with configured max size and idle timeout

#### Scenario: Pool with custom size
- **WHEN** MCP server configuration specifies custom pool size
- **THEN** daemon creates pool with the specified maximum connections

### Requirement: Connection lifecycle management
The connection pool SHALL manage HTTP connection lifecycle including creation, reuse, and cleanup.

#### Scenario: Acquire available connection
- **WHEN** daemon needs connection and pool has idle connection
- **THEN** pool returns existing connection without creating new one

#### Scenario: Acquire when pool is empty
- **WHEN** daemon needs connection and pool has no idle connections
- **THEN** pool creates new connection if under max size limit

#### Scenario: Acquire when pool is full
- **WHEN** daemon needs connection and pool is at max size
- **THEN** daemon waits for available connection or times out after configured duration

### Requirement: Idle connection timeout
The connection pool SHALL close HTTP connections that have been idle for longer than configured timeout.

#### Scenario: Connection idle timeout
- **WHEN** connection has been idle for longer than configured timeout (default 5 minutes)
- **THEN** pool closes the connection and removes it from pool

#### Scenario: Connection used before timeout
- **WHEN** connection is reused before idle timeout expires
- **THEN** pool resets the idle timer for that connection

### Requirement: Connection validation
The connection pool SHALL validate connections before reuse to ensure they are still healthy.

#### Scenario: Validate before reuse
- **WHEN** pool returns idle connection for reuse
- **THEN** pool verifies connection is still open and responsive

#### Scenario: Invalid connection
- **WHEN** validation detects a closed or broken connection
- **THEN** pool removes connection and creates a new one

### Requirement: Maximum connection lifetime
The connection pool SHALL enforce maximum lifetime for connections to prevent stale connections.

#### Scenario: Connection exceeds max lifetime
- **WHEN** connection has been active for longer than max lifetime (default 30 minutes)
- **THEN** pool closes connection after current request completes and creates new connection

#### Scenario: Connection within lifetime
- **WHEN** connection is under max lifetime limit
- **THEN** pool continues to reuse the connection

### Requirement: Connection pool shutdown
The connection pool SHALL gracefully close all connections when daemon stops.

#### Scenario: Daemon shutdown
- **WHEN** daemon receives shutdown signal
- **THEN** pool waits for in-flight requests to complete and closes all connections

#### Scenario: Forced shutdown
- **WHEN** daemon forced termination occurs
- **THEN** pool immediately closes all connections without waiting

### Requirement: Pool metrics
The connection pool SHALL track metrics for monitoring and debugging connection usage.

#### Scenario: Report pool statistics
- **WHEN** daemon status is queried
- **THEN** pool reports active connections, idle connections, and total created connections

#### Scenario: Connection leak detection
- **WHEN** pool approaches max size consistently
- **THEN** pool logs warning about potential connection leaks
