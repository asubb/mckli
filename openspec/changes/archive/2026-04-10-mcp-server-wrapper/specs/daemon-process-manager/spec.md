## ADDED Requirements

### Requirement: Daemon process spawning
The system SHALL spawn a separate daemon process for each configured MCP server instance.

#### Scenario: Start daemon for new MCP server
- **WHEN** user starts a daemon for a configured MCP server
- **THEN** system spawns a new process and records PID in config directory

#### Scenario: Daemon already running
- **WHEN** user attempts to start a daemon that is already running
- **THEN** system returns an error indicating the daemon is active

#### Scenario: Daemon process fails to start
- **WHEN** daemon spawn fails due to system error
- **THEN** system returns detailed error message and cleans up partial state

### Requirement: PID file management
The system SHALL maintain PID files in `~/.mckli/daemons/` for tracking running daemon processes.

#### Scenario: Create PID file on daemon start
- **WHEN** daemon process is successfully spawned
- **THEN** system writes PID file with process ID and server identifier

#### Scenario: Remove PID file on daemon stop
- **WHEN** daemon is stopped gracefully
- **THEN** system removes the corresponding PID file

#### Scenario: Stale PID file cleanup
- **WHEN** PID file exists but process is not running
- **THEN** system removes the stale PID file on next CLI invocation

### Requirement: Daemon lifecycle commands
The system SHALL provide CLI commands for daemon lifecycle management: start, stop, status, and restart.

#### Scenario: Start command
- **WHEN** user runs `mckli daemon start <server-name>`
- **THEN** system spawns daemon for the specified server and confirms success

#### Scenario: Stop command
- **WHEN** user runs `mckli daemon stop <server-name>`
- **THEN** system sends termination signal to daemon and waits for graceful shutdown

#### Scenario: Status command
- **WHEN** user runs `mckli daemon status`
- **THEN** system lists all configured servers with daemon running status

#### Scenario: Restart command
- **WHEN** user runs `mckli daemon restart <server-name>`
- **THEN** system stops existing daemon and starts a new one

### Requirement: Graceful shutdown
Daemon processes SHALL handle termination signals and shutdown gracefully, closing active connections.

#### Scenario: SIGTERM received
- **WHEN** daemon receives SIGTERM signal
- **THEN** daemon closes HTTP connections, releases resources, and exits with code 0

#### Scenario: Forced termination
- **WHEN** daemon does not exit within 10 seconds of SIGTERM
- **THEN** system sends SIGKILL to force termination

### Requirement: Process health monitoring
The system SHALL verify daemon process health by checking PID existence and responsiveness.

#### Scenario: Check process is running
- **WHEN** system checks daemon status using PID file
- **THEN** system verifies the process exists in the OS process table

#### Scenario: Daemon process crashed
- **WHEN** PID file exists but process is not running
- **THEN** system reports daemon as crashed and offers restart option

### Requirement: Daemon auto-start
The system SHALL automatically start daemons on first request if not already running.

#### Scenario: Request to inactive daemon
- **WHEN** CLI sends request to server with no running daemon
- **THEN** system starts daemon automatically and retries the request

#### Scenario: Auto-start failure
- **WHEN** auto-start fails due to configuration or system error
- **THEN** system returns error and does not retry request

### Requirement: Multi-daemon support
The system SHALL support multiple daemon processes running concurrently for different MCP servers.

#### Scenario: Start multiple daemons
- **WHEN** multiple MCP servers are configured
- **THEN** system can start independent daemon process for each server

#### Scenario: Isolated daemon failures
- **WHEN** one daemon crashes or stops
- **THEN** other daemons continue running unaffected
